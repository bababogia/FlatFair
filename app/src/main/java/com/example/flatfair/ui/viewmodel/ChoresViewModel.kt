package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChoresViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val choreRepository: ChoreRepository,
    private val householdRepository: HouseholdRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentHousehold = currentUser.flatMapLatest { user ->
        if (user?.currentHouseholdId != null) {
            householdRepository.getHousehold(user.currentHouseholdId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAdmin = combine(currentUser, currentHousehold) { user, household ->
        household?.adminIds?.contains(user?.id) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availableChores: StateFlow<List<Chore>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                choreRepository.getChoresForHousehold(user.currentHouseholdId)
                    .map { chores -> chores.filter { it.status == ChoreStatus.PENDING } }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignedChores: StateFlow<List<Chore>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                choreRepository.getAssignedChores(user.id)
                    .map { chores -> chores.filter { it.status == ChoreStatus.ASSIGNED || it.status == ChoreStatus.WAITING_FOR_VERIFICATION } }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roommateChores: StateFlow<List<Chore>> = combine(currentUser, currentHousehold) { user, household ->
        if (user != null && household != null) {
            choreRepository.getChoresForHousehold(household.id)
                .map { chores -> chores.filter { it.assignedUserId != null && it.assignedUserId != user.id && (it.status == ChoreStatus.ASSIGNED || it.status == ChoreStatus.WAITING_FOR_VERIFICATION) } }
        } else {
            flowOf(emptyList())
        }
    }.flattenConcat()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val choresToVerify: StateFlow<List<Chore>> = currentHousehold
        .flatMapLatest { household ->
            if (household != null) {
                choreRepository.getPendingVerifications(household.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedChores: StateFlow<List<Chore>> = currentHousehold
        .flatMapLatest { household ->
            if (household != null) {
                choreRepository.getChoresForHousehold(household.id)
                    .map { chores -> chores.filter { it.status == ChoreStatus.COMPLETED }.sortedByDescending { it.verifiedAt ?: 0L } }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createChore(
        title: String, 
        description: String, 
        difficulty: Int, 
        frequency: ChoreFrequency = ChoreFrequency.ONCE, 
        category: String = "General",
        deadline: Date? = null
    ) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return
        viewModelScope.launch {
            val chore = Chore(
                title = title,
                description = description,
                difficulty = difficulty,
                householdId = householdId,
                status = ChoreStatus.PENDING,
                baseReward = difficulty * 10,
                frequency = frequency,
                category = category,
                deadline = deadline
            )
            val result = choreRepository.createChore(chore)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = householdId,
                    userId = user.id,
                    userName = user.name,
                    action = "created a new ${if (frequency == ChoreFrequency.ONCE) "" else frequency.name.lowercase() + " "}chore:",
                    targetName = title,
                    type = ActivityType.CHORE
                ))
            }
        }
    }

    fun deleteChore(chore: Chore) {
        viewModelScope.launch {
            choreRepository.deleteChore(chore.id)
        }
    }

    fun placeBid(choreId: String, amount: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.placeBid(Bid(user.id, user.name, choreId, amount))
            if (result.isSuccess) {
                val chore = (availableChores.value + assignedChores.value + roommateChores.value).find { it.id == choreId }
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "placed a bid to avoid:",
                    targetName = chore?.title ?: "a chore",
                    type = ActivityType.BID
                ))
            }
        }
    }

    fun requestVerification(chore: Chore, proofImageUri: android.net.Uri? = null) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            if (proofImageUri != null) {
                val uploadResult = choreRepository.uploadProofImage(chore.id, proofImageUri)
                if (uploadResult.isSuccess) {
                    choreRepository.updateChoreProof(chore.id, uploadResult.getOrThrow())
                }
            }
            choreRepository.updateChoreStatus(chore.id, ChoreStatus.WAITING_FOR_VERIFICATION)
        }
    }

    fun approveChore(chore: Chore, rating: Int, feedback: String?) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.updateChoreStatus(chore.id, ChoreStatus.COMPLETED)
            if (result.isSuccess) {
                choreRepository.submitRating(chore.id, rating, feedback)
                
                chore.assignedUserId?.let { userId ->
                    val bonus = if (rating == 5) (chore.totalReward * 0.2).toInt() else 0
                    val finalReward = chore.totalReward + bonus
                    
                    userRepository.updateCredits(userId, finalReward)
                    transactionRepository.addTransaction(
                        Transaction(
                            userId = userId,
                            amount = finalReward,
                            type = TransactionType.TASK_REWARD,
                            choreId = chore.id
                        )
                    )
                    
                    notificationRepository.sendNotification(Notification(
                        userId = userId,
                        title = "Chore Approved!",
                        body = "Your work on '${chore.title}' was approved ($rating stars). You earned $finalReward credits.",
                        type = NotificationType.CHORE_APPROVED,
                        relatedId = chore.id
                    ))

                    activityRepository.logActivity(ActivityLog(
                        householdId = admin.currentHouseholdId ?: "",
                        userId = admin.id,
                        userName = admin.name,
                        action = "approved completion of ${chore.title} with a $rating-star rating.",
                        targetName = chore.title,
                        type = ActivityType.CHORE
                    ))

                    if (chore.frequency != ChoreFrequency.ONCE) {
                        val nextChore = chore.copy(
                            id = "", 
                            assignedUserId = null,
                            assignedUserName = null,
                            status = ChoreStatus.PENDING,
                            bonusPot = 0,
                            proofImageUrl = null,
                            rating = null,
                            feedback = null
                        )
                        choreRepository.createChore(nextChore)
                    }
                }
            }
        }
    }

    fun rejectVerification(chore: Chore) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.updateChoreStatus(chore.id, ChoreStatus.ASSIGNED)
            if (result.isSuccess) {
                chore.assignedUserId?.let { userId ->
                    notificationRepository.sendNotification(Notification(
                        userId = userId,
                        title = "Chore Rejected",
                        body = "The proof for '${chore.title}' was not sufficient. Please try again.",
                        type = NotificationType.NUDGE,
                        relatedId = chore.id
                    ))
                }
            }
        }
    }

    fun resolveBidding(chore: Chore) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val bids = choreRepository.getBidsForChore(chore.id).first()
            if (bids.isEmpty()) return@launch

            val minBidAmount = bids.minOf { it.amount }
            val potentialWinners = bids.filter { it.amount == minBidAmount }
            val winnerBid = potentialWinners.random()
            
            // The bonus pot is the sum of credits people were willing to pay to AVOID the chore.
            // The winner (who bid the least) doesn't pay their own bid.
            val otherBids = bids.filter { it.userId != winnerBid.userId }
            val totalBonus = otherBids.sumOf { it.amount }
            
            val result = choreRepository.assignChore(chore.id, winnerBid.userId, winnerBid.userName, totalBonus)
            
            if (result.isSuccess) {
                notificationRepository.sendNotification(Notification(
                    userId = winnerBid.userId,
                    title = "New Task Assigned",
                    body = "You've been assigned '${chore.title}' because you had the lowest bid to avoid it. Bonus: $totalBonus credits.",
                    type = NotificationType.CHORE_ASSIGNED,
                    relatedId = chore.id
                ))

                // Deduct credits from everyone except the winner
                otherBids.forEach { bid ->
                    if (bid.amount > 0) {
                        userRepository.updateCredits(bid.userId, -bid.amount)
                        transactionRepository.addTransaction(
                            Transaction(
                                userId = bid.userId,
                                amount = bid.amount,
                                type = TransactionType.BID_SPENT,
                                choreId = chore.id
                            )
                        )
                    }
                }
                
                activityRepository.logActivity(ActivityLog(
                    householdId = admin.currentHouseholdId ?: "",
                    userId = admin.id,
                    userName = admin.name,
                    action = "resolved bidding for ${chore.title}. Assigned to ${winnerBid.userName}.",
                    targetName = chore.title,
                    type = ActivityType.BID
                ))
            }
        }
    }

    fun nudgeUser(chore: Chore) {
        val nudger = currentUser.value ?: return
        viewModelScope.launch {
            activityRepository.logActivity(ActivityLog(
                householdId = nudger.currentHouseholdId ?: "",
                userId = nudger.id,
                userName = nudger.name,
                action = "nudged ${chore.assignedUserName} to finish:",
                targetName = chore.title,
                type = ActivityType.NUDGE
            ))
            
            chore.assignedUserId?.let { userId ->
                notificationRepository.sendNotification(Notification(
                    userId = userId,
                    title = "Friendly Nudge!",
                    body = "${nudger.name} is checking in on '${chore.title}'.",
                    type = NotificationType.NUDGE,
                    relatedId = chore.id
                ))
            }
            
            choreRepository.addComment(chore.id, Comment(
                authorId = "system",
                authorName = "FlatFair Bot",
                content = "${nudger.name} sent a friendly nudge! How's it going with this task?"
            ))
        }
    }

    fun getBidsForChore(choreId: String): Flow<List<Bid>> = choreRepository.getBidsForChore(choreId)
    fun getCommentsForChore(choreId: String): Flow<List<Comment>> = choreRepository.getCommentsForChore(choreId)

    fun addComment(choreId: String, content: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            choreRepository.addComment(choreId, Comment(authorId = user.id, authorName = user.name, content = content))
        }
    }
}
