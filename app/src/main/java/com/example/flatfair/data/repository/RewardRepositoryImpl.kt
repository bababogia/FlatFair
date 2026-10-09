package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Reward
import com.example.flatfair.domain.repository.RewardRepository
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
}
