package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChoresViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val choreRepository: ChoreRepository,
    private val householdRepository: HouseholdRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val activityRepository: ActivityRepository
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
                    .map { chores -> chores.filter { it.status == ChoreStatus.COMPLETED }.sortedByDescending { it.deadline } }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createChore(title: String, description: String, difficulty: Int, frequency: ChoreFrequency = ChoreFrequency.ONCE) {
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
                frequency = frequency
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
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.deleteChore(chore.id)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "deleted the chore:",
                    targetName = chore.title,
                    type = ActivityType.CHORE
                ))
            }
        }
    }

    fun placeBid(choreId: String, amount: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.placeBid(
                Bid(
                    userId = user.id,
                    userName = user.name,
                    choreId = choreId,
                    amount = amount
                )
            )
            if (result.isSuccess) {
                val chore = availableChores.value.find { it.id == choreId }
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

    fun requestVerification(chore: Chore) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.updateChoreStatus(chore.id, ChoreStatus.WAITING_FOR_VERIFICATION)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "finished and requested verification for:",
                    targetName = chore.title,
                    type = ActivityType.CHORE
                ))
            }
        }
    }

    fun approveChore(chore: Chore) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val result = choreRepository.updateChoreStatus(chore.id, ChoreStatus.COMPLETED)
            if (result.isSuccess) {
                chore.assignedUserId?.let { userId ->
                    val totalReward = chore.totalReward
                    userRepository.updateCredits(userId, totalReward)
                    transactionRepository.addTransaction(
                        Transaction(
                            userId = userId,
                            amount = totalReward,
                            type = TransactionType.TASK_REWARD,
                            choreId = chore.id
                        )
                    )
                    
                    activityRepository.logActivity(ActivityLog(
                        householdId = admin.currentHouseholdId ?: "",
                        userId = admin.id,
                        userName = admin.name,
                        action = "approved completion of:",
                        targetName = chore.title,
                        type = ActivityType.CHORE
                    ))

                    if (chore.frequency != ChoreFrequency.ONCE) {
                        val nextChore = chore.copy(
                            id = "", 
                            assignedUserId = null,
                            assignedUserName = null,
                            status = ChoreStatus.PENDING,
                            bonusPot = 0
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
            choreRepository.updateChoreStatus(chore.id, ChoreStatus.ASSIGNED)
            activityRepository.logActivity(ActivityLog(
                householdId = admin.currentHouseholdId ?: "",
                userId = admin.id,
                userName = admin.name,
                action = "rejected verification for:",
                targetName = chore.title,
                type = ActivityType.CHORE
            ))
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
            
            val totalBonus = bids.sumOf { it.amount }
            
            val result = choreRepository.assignChore(chore.id, winnerBid.userId, winnerBid.userName, totalBonus)
            
            if (result.isSuccess) {
                bids.forEach { bid ->
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
                    action = "resolved bidding for: ${chore.title}. Assigned to ${winnerBid.userName} with a ${totalBonus} credit bonus.",
                    targetName = winnerBid.userName,
                    type = ActivityType.HOUSEHOLD
                ))
            }
        }
    }

    fun getBidsForChore(choreId: String): Flow<List<Bid>> {
        return choreRepository.getBidsForChore(choreId)
    }
}
