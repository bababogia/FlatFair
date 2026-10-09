package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Transaction
import com.example.flatfair.domain.repository.TransactionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TransactionRepository {

    override fun getTransactionsForUser(userId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()
                trySend(transactions)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Unit> = try {
        val docRef = firestore.collection("transactions").document()
        val newTransaction = transaction.copy(id = docRef.id)
        docRef.set(newTransaction).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
