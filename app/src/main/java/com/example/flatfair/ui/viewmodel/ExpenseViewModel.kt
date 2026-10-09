package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.Expense
import com.example.flatfair.domain.model.Transaction
import com.example.flatfair.domain.model.TransactionType
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.ExpenseRepository
import com.example.flatfair.domain.repository.TransactionRepository
import com.example.flatfair.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val transactionRepository: TransactionRepository
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

    // Calculate how much is owed to/by the current user
    val userBalances: StateFlow<List<UserBalance>> = combine(currentUser, expenses) { user, expenseList ->
        if (user == null) return@combine emptyList()
        
        val balanceMap = mutableMapOf<String, Double>() // userId -> amount
        val nameMap = mutableMapOf<String, String>()

        expenseList.filter { !it.isSettled }.forEach { expense ->
            val share = expense.amount / (expense.splitWithUserIds.size + 1)
            
            if (expense.paidByUserId == user.id) {
                // Current user paid, others owe them
                expense.splitWithUserIds.forEach { otherId ->
                    balanceMap[otherId] = (balanceMap[otherId] ?: 0.0) + share
                    // We don't have names for everyone here easily, 
                    // in a real app we'd fetch household members once and cache names.
                }
            } else if (expense.splitWithUserIds.contains(user.id)) {
                // Someone else paid, current user owes them
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
            expenseRepository.addExpense(expense)
        }
    }

    fun settleExpense(expense: Expense) {
        viewModelScope.launch {
            val result = expenseRepository.settleExpense(expense.id)
            if (result.isSuccess) {
                val share = expense.amount / (expense.splitWithUserIds.size + 1)
                val shareInCredits = (share * 10).toInt() 

                expense.splitWithUserIds.forEach { userId ->
                    // Deduct credits from those who owed
                    userRepository.updateCredits(userId, -shareInCredits)
                    transactionRepository.addTransaction(
                        Transaction(
                            userId = userId,
                            amount = shareInCredits,
                            type = TransactionType.EXPENSE_SETTLEMENT,
                            expenseId = expense.id
                        )
                    )

                    // Add credits to the one who paid
                    userRepository.updateCredits(expense.paidByUserId, shareInCredits)
                    transactionRepository.addTransaction(
                        Transaction(
                            userId = expense.paidByUserId,
                            amount = shareInCredits,
                            type = TransactionType.EXPENSE_SETTLEMENT,
                            expenseId = expense.id
                        )
                    )
                }
            }
        }
    }
}
