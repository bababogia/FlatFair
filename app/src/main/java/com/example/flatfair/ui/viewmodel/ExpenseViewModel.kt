package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class UserBalance(
    val otherUserId: String,
    val otherUserName: String,
    val netAmount: Double // Positive means they owe you, negative means you owe them
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val expenses: StateFlow<List<Expense>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                expenseRepository.getExpensesForHousehold(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBalances: StateFlow<List<UserBalance>> = combine(currentUser, expenses) { user, expenseList ->
        if (user == null) return@combine emptyList()
        
        val balanceMap = mutableMapOf<String, Double>()
        val nameMap = mutableMapOf<String, String>()

        expenseList.filter { !it.isSettled }.forEach { expense ->
            val share = expense.amount / (expense.splitWithUserIds.size + 1)
            
            if (expense.paidByUserId == user.id) {
                expense.splitWithUserIds.forEach { otherId ->
                    balanceMap[otherId] = (balanceMap[otherId] ?: 0.0) + share
                }
            } else if (expense.splitWithUserIds.contains(user.id)) {
                balanceMap[expense.paidByUserId] = (balanceMap[expense.paidByUserId] ?: 0.0) - share
                nameMap[expense.paidByUserId] = expense.paidByUserName
            }
        }

        balanceMap.map { (id, amount) ->
            UserBalance(id, nameMap[id] ?: "Roommate", amount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(title: String, amount: Double, splitWithUserIds: List<String>) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return
        
        viewModelScope.launch {
            val expense = Expense(
                householdId = householdId,
                title = title,
                amount = amount,
                paidByUserId = user.id,
                paidByUserName = user.name,
                splitWithUserIds = splitWithUserIds
            )
            val result = expenseRepository.addExpense(expense)
            if (result.isSuccess) {
                // Log Activity
                activityRepository.logActivity(ActivityLog(
                    householdId = householdId,
                    userId = user.id,
                    userName = user.name,
                    action = "logged an expense:",
                    targetName = String.format(Locale.US, "%s ($%.2f)", title, amount),
                    type = ActivityType.EXPENSE
                ))

                // Notify Participants
                splitWithUserIds.forEach { participantId ->
                    notificationRepository.sendNotification(Notification(
                        userId = participantId,
                        title = "New Shared Expense",
                        body = String.format(Locale.US, "%s added '%s'. Your share is $%.2f", user.name, title, amount / (splitWithUserIds.size + 1)),
                        type = NotificationType.EXPENSE_ADDED
                    ))
                }
            }
        }
    }

    fun settleExpense(expense: Expense) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = expenseRepository.settleExpense(expense.id)
            if (result.isSuccess) {
                val share = expense.amount / (expense.splitWithUserIds.size + 1)
                val shareInCredits = (share * 10).toInt() // Conversion rate: $1 = 10 credits

                // 1. Log activity
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "settled the expense:",
                    targetName = expense.title,
                    type = ActivityType.EXPENSE
                ))

                // 2. Adjust credits for each participant
                expense.splitWithUserIds.forEach { userId ->
                    userRepository.updateCredits(userId, -shareInCredits)
                    transactionRepository.addTransaction(Transaction(
                        userId = userId,
                        amount = shareInCredits,
                        type = TransactionType.EXPENSE_SETTLEMENT,
                        expenseId = expense.id
                    ))
                }

                // 3. Reward the person who paid
                val totalRecouped = shareInCredits * expense.splitWithUserIds.size
                userRepository.updateCredits(expense.paidByUserId, totalRecouped)
                transactionRepository.addTransaction(Transaction(
                    userId = expense.paidByUserId,
                    amount = totalRecouped,
                    type = TransactionType.EXPENSE_SETTLEMENT,
                    expenseId = expense.id
                ))
            }
        }
    }
}
