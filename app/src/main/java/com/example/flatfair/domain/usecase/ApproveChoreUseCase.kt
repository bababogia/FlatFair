package com.example.flatfair.domain.usecase

import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.*
import javax.inject.Inject

/**
 * Handles the logic for an Admin approving a completed chore.
 */
class ApproveChoreUseCase @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(
        chore: Chore,
        rating: Int,
        feedback: String?,
        adminId: String,
        adminName: String
    ): Result<Unit> {
        val result = choreRepository.updateChoreStatus(chore.id, ChoreStatus.COMPLETED)
        if (result.isFailure) return result

        // 1. Submit rating and feedback
        choreRepository.submitRating(chore.id, rating, feedback)
        
        val assigneeId = chore.assignedUserId ?: return Result.failure(Exception("No assignee found"))
        
        // 2. Calculate Final Reward
        // Rule: 5-star rating gives a 20% performance bonus
        val performanceBonus = if (rating == 5) (chore.totalReward * 0.2).toInt() else 0
        val finalReward = chore.totalReward + performanceBonus
        
        // 3. Award Credits
        userRepository.updateCredits(assigneeId, finalReward)
        
        // 4. Record Transaction
        transactionRepository.addTransaction(
            Transaction(
                userId = assigneeId,
                amount = finalReward,
                type = TransactionType.TASK_REWARD,
                choreId = chore.id
            )
        )
        
        // 5. Notify the User
        notificationRepository.sendNotification(Notification(
            userId = assigneeId,
            title = "Chore Approved!",
            body = "Your work on '${chore.title}' was approved ($rating stars). You earned $finalReward credits.",
            type = NotificationType.CHORE_APPROVED,
            relatedId = chore.id
        ))

        // 6. Log Activity
        activityRepository.logActivity(ActivityLog(
            householdId = chore.householdId,
            userId = adminId,
            userName = adminName,
            action = "approved completion of ${chore.title} with a $rating-star rating.",
            targetName = chore.title,
            type = ActivityType.CHORE
        ))

        // 7. Handle Recurring Chores
        if (chore.frequency != ChoreFrequency.ONCE) {
            val nextChore = chore.copy(
                id = "", 
                assignedUserId = null,
                assignedUserName = null,
                status = ChoreStatus.PENDING,
                bonusPot = 0,
                proofImageUrl = null,
                rating = null,
                feedback = null,
                completedAt = null,
                verifiedAt = null
            )
            choreRepository.createChore(nextChore)
        }
        
        return Result.success(Unit)
    }
}
