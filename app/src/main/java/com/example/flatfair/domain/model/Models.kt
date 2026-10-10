package com.example.flatfair.domain.model

import java.util.Date

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val currentHouseholdId: String? = null,
    val creditBalance: Int = 0,
    val isHome: Boolean = true,
    val lastActive: Long = System.currentTimeMillis()
)

data class Household(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val houseRules: String = ""
)

data class Chore(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: Int = 1, // 1-5
    val baseReward: Int = 10,
    val bonusPot: Int = 0, // Credits collected from avoid-bids
    val deadline: Date? = null,
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val status: ChoreStatus = ChoreStatus.PENDING,
    val category: String = "General",
    val frequency: ChoreFrequency = ChoreFrequency.ONCE,
    val repeatDay: Int? = null, 
    val proofImageUrl: String? = null,
    val rating: Int? = null, // 1-5 stars
    val feedback: String? = null,
    val completedAt: Long? = null,
    val verifiedAt: Long? = null
) {
    val totalReward: Int get() = baseReward + bonusPot
}

enum class ChoreFrequency {
    ONCE,
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}

enum class ChoreStatus {
    PENDING,                // Created, not yet assigned (bidding phase)
    ASSIGNED,               // Assigned to a user, work in progress
    WAITING_FOR_VERIFICATION, // Finished by user, needs approval
    COMPLETED,              // Approved and credits awarded
    OVERDUE                 // Missed deadline
}

data class Bid(
    val userId: String = "",
    val userName: String = "", 
    val choreId: String = "",
    val amount: Int = 0
)

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Expense(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidByUserId: String = "",
    val paidByUserName: String = "",
    val date: Long = System.currentTimeMillis(),
    val splitWithUserIds: List<String> = emptyList(),
    val isSettled: Boolean = false
)

data class ShoppingItem(
    val id: String = "",
    val householdId: String = "",
    val name: String = "",
    val quantity: String = "",
    val addedByUserId: String = "",
    val addedByUserName: String = "",
    val isUrgent: Boolean = false,
    val isBought: Boolean = false
)

data class Announcement(
    val id: String = "",
    val householdId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isImportant: Boolean = false
)

data class AnonymousPost(
    val id: String = "",
    val householdId: String = "",
    val authorId: String = "", // Kept for moderation/deletion by author
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val authorAlias: String = "Mysterious Roommate"
)

data class Event(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + 3600000, 
    val createdByUserId: String = "",
    val createdByUserName: String = "",
    val type: EventType = EventType.GENERAL
)

enum class EventType {
    GENERAL,
    GUEST,
    AWAY,
    CLEANING_DAY,
    MEETING
}

data class Poll(
    val id: String = "",
    val householdId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val votes: Map<String, Int> = emptyMap(), // userId -> optionIndex
    val createdByUserId: String = "",
    val createdByUserName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false
)

data class Reward(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val description: String = "",
    val cost: Int = 0,
    val createdByUserId: String = "",
    val isAvailable: Boolean = true
)

data class Notification(
    val id: String = "",
    val userId: String = "", // Recipient
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.NUDGE,
    val relatedId: String? = null 
)

enum class NotificationType {
    NUDGE,
    CHORE_ASSIGNED,
    CHORE_APPROVED,
    EXPENSE_ADDED,
    POLL_STARTED,
    ANNOUNCEMENT_POSTED,
    EVENT_ADDED,
    ANONYMOUS_POST_ADDED
}

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Int = 0,
    val type: TransactionType = TransactionType.STIPEND,
    val timestamp: Long = System.currentTimeMillis(),
    val choreId: String? = null,
    val expenseId: String? = null,
    val rewardId: String? = null
)

enum class TransactionType {
    BID_SPENT,
    TASK_REWARD,
    STIPEND,
    EXPENSE_SETTLEMENT,
    REWARD_REDEMPTION
}

data class ActivityLog(
    val id: String = "",
    val householdId: String = "",
    val userId: String = "",
    val userName: String = "",
    val action: String = "", 
    val targetName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: ActivityType = ActivityType.CHORE
)

enum class ActivityType {
    CHORE,
    EXPENSE,
    BID,
    HOUSEHOLD,
    SHOPPING,
    ANNOUNCEMENT,
    ANONYMOUS_POST,
    NUDGE,
    EVENT,
    REWARD,
    STATUS,
    POLL
}
