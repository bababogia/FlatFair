package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForHousehold(householdId: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Unit>
    suspend fun settleExpense(expenseId: String): Result<Unit>
}
