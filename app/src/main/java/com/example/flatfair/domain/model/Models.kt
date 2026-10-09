package com.example.flatfair.domain.model

import java.util.Date

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val currentHouseholdId: String? = null,
    val creditBalance: Int = 0
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
    val repeatDay: Int? = null, // 1-7 for day of week
    val proofImageUrl: String? = null
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

data class Event(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + 3600000, // +1 hour
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

data class Reward(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val description: String = "",
    val cost: Int = 0,
    val createdByUserId: String = "",
    val isAvailable: Boolean = true
)

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
    NUDGE,
    EVENT,
    REWARD
}
