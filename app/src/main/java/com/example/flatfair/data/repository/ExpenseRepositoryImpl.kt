package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Expense
import com.example.flatfair.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    override fun getExpensesForHousehold(householdId: String): Flow<List<Expense>> = callbackFlow {
        val subscription = firestore.collection("expenses")
            .whereEqualTo("householdId", householdId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(expenses)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addExpense(expense: Expense): Result<Unit> = try {
        val docRef = firestore.collection("expenses").document()
        val newExpense = expense.copy(id = docRef.id)
        docRef.set(newExpense).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun settleExpense(expenseId: String): Result<Unit> = try {
        firestore.collection("expenses").document(expenseId)
            .update("isSettled", true)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
