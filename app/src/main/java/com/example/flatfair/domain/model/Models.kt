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
    val memberIds: List<String> = emptyList()
)

data class Chore(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: Int = 1, // 1-5
    val deadline: Date? = null,
    val assignedUserId: String? = null,
    val status: ChoreStatus = ChoreStatus.PENDING,
    val category: String = "General"
)

enum class ChoreStatus {
    PENDING,    // Created, not yet assigned (bidding phase)
    ASSIGNED,   // Assigned to a user
    COMPLETED,  // Finished and verified
    OVERDUE     // Missed deadline
}

data class Bid(
    val userId: String = "",
    val choreId: String = "",
    val amount: Int = 0
)

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Int = 0,
    val type: TransactionType = TransactionType.STIPEND,
    val timestamp: Long = System.currentTimeMillis(),
    val choreId: String? = null
)

enum class TransactionType {
    BID_SPENT,
    TASK_REWARD,
    STIPEND,
    EXPENSE_SETTLEMENT
}
