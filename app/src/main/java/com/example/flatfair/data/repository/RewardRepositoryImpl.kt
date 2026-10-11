package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.ActivityLog
import com.example.flatfair.domain.model.Reward
import com.example.flatfair.domain.model.Transaction
import com.example.flatfair.domain.repository.RewardRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RewardRepository {

    override fun getRewards(householdId: String): Flow<List<Reward>> = callbackFlow {
        val subscription = firestore.collection("rewards")
            .whereEqualTo("householdId", householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val rewards = snapshot?.toObjects(Reward::class.java) ?: emptyList()
                trySend(rewards)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addReward(reward: Reward): Result<Unit> = try {
        val docRef = firestore.collection("rewards").document()
        val newReward = reward.copy(id = docRef.id)
        docRef.set(newReward).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateReward(reward: Reward): Result<Unit> = try {
        firestore.collection("rewards").document(reward.id).set(reward).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteReward(rewardId: String): Result<Unit> = try {
        firestore.collection("rewards").document(rewardId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleRewardAvailability(rewardId: String, isAvailable: Boolean): Result<Unit> = try {
        firestore.collection("rewards").document(rewardId)
            .update("isAvailable", isAvailable)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun redeemReward(
        userId: String,
        reward: Reward,
        transaction: Transaction,
        activityLog: ActivityLog
    ): Result<Unit> = try {
        firestore.runTransaction { firestoreTransaction ->
            val userRef = firestore.collection("users").document(userId)
            val transactionRef = firestore.collection("transactions").document()
            val activityRef = firestore.collection("activity").document()

            // 1. Deduct credits
            firestoreTransaction.update(userRef, "creditBalance", FieldValue.increment(-reward.cost.toLong()))

            // 2. Add transaction record
            val newTransaction = transaction.copy(id = transactionRef.id)
            firestoreTransaction.set(transactionRef, newTransaction)

            // 3. Log activity
            val newActivity = activityLog.copy(id = activityRef.id)
            firestoreTransaction.set(activityRef, newActivity)
            
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
