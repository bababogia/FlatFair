package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsForUser(userId: String): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction): Result<Unit>
}
