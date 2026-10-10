package com.example.flatfair.domain.usecase

import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Implements the Bidding & Allocation System logic.
 *
 * Algorithm: Reverse Auction (Vickrey-style influence)
 * 1. Roommates bid credits they are willing to pay to AVOID a chore.
 * 2. The person with the LOWEST bid "wins" (is assigned) the chore.
 * 3. All other roommates pay their bids into a bonus pot.
 * 4. The winner receives the (Base Reward + Bonus Pot) upon completion.
 *
 * Why this is fair:
 * - High bidders pay to skip tasks they hate.
 * - Low bidders get extra credits for doing tasks they don't mind.
 */
class ResolveBiddingUseCase @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationRepository: NotificationRepository,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(chore: Chore, adminId: String, adminName: String): Result<Unit> {
        val bids = choreRepository.getBidsForChore(chore.id).first()
        if (bids.isEmpty()) return Result.failure(Exception("No bids found for this chore"))

        // 1. Find the lowest bid amount
        val minBidAmount = bids.minOf { it.amount }
        
        // 2. Identify potential winners (those who bid the minimum)
        val potentialWinners = bids.filter { it.amount == minBidAmount }
        
        // 3. Select a winner (randomly if tie)
        val winnerBid = potentialWinners.random()
        
        // 4. Calculate the bonus pot from everyone else
        val otherBids = bids.filter { it.userId != winnerBid.userId }
        val totalBonus = otherBids.sumOf { it.amount }
        
        // 5. Assign the chore in the repository
        val assignResult = choreRepository.assignChore(
            choreId = chore.id,
            userId = winnerBid.userId,
            userName = winnerBid.userName,
            bonusPot = totalBonus
        )
        
        if (assignResult.isSuccess) {
            // 6. Deduct credits from roommates who avoided the chore
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

            // 7. Notify the winner
            notificationRepository.sendNotification(
                Notification(
                    userId = winnerBid.userId,
                    title = "Task Assigned!",
                    body = "You've been assigned '${chore.title}'. Bonus pot: $totalBonus credits.",
                    type = NotificationType.CHORE_ASSIGNED,
                    relatedId = chore.id
                )
            )

            // 8. Log the activity
            activityRepository.logActivity(
                ActivityLog(
                    householdId = chore.householdId,
                    userId = adminId,
                    userName = adminName,
                    action = "resolved bidding for ${chore.title}. Assigned to ${winnerBid.userName}.",
                    targetName = chore.title,
                    type = ActivityType.BID
                )
            )
            
            return Result.success(Unit)
        } else {
            return assignResult
        }
    }
}
