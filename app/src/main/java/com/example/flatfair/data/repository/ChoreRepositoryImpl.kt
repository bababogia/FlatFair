package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.Bid
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.repository.ChoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChoreRepository {

    override fun getChoresForHousehold(householdId: String): Flow<List<Chore>> = callbackFlow {
        val subscription = firestore.collection("chores")
            .whereEqualTo("householdId", householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val chores = snapshot?.toObjects(Chore::class.java) ?: emptyList()
                trySend(chores)
            }
        awaitClose { subscription.remove() }
    }

    override fun getAssignedChores(userId: String): Flow<List<Chore>> = callbackFlow {
        val subscription = firestore.collection("chores")
            .whereEqualTo("assignedUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val chores = snapshot?.toObjects(Chore::class.java) ?: emptyList()
                trySend(chores)
            }
        awaitClose { subscription.remove() }
    }

    override fun getBidsForChore(choreId: String): Flow<List<Bid>> = callbackFlow {
        val subscription = firestore.collection("chores").document(choreId)
            .collection("bids")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val bids = snapshot?.toObjects(Bid::class.java) ?: emptyList()
                trySend(bids)
            }
        awaitClose { subscription.remove() }
    }

    override fun getPendingVerifications(householdId: String): Flow<List<Chore>> = callbackFlow {
        val subscription = firestore.collection("chores")
            .whereEqualTo("householdId", householdId)
            .whereEqualTo("status", ChoreStatus.WAITING_FOR_VERIFICATION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val chores = snapshot?.toObjects(Chore::class.java) ?: emptyList()
                trySend(chores)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createChore(chore: Chore): Result<Unit> = try {
        val docRef = firestore.collection("chores").document()
        val newChore = chore.copy(id = docRef.id)
        docRef.set(newChore).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun placeBid(bid: Bid): Result<Unit> = try {
        firestore.collection("chores").document(bid.choreId)
            .collection("bids").document(bid.userId).set(bid).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateChoreStatus(choreId: String, status: ChoreStatus): Result<Unit> = try {
        firestore.collection("chores").document(choreId)
            .update("status", status).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun assignChore(choreId: String, userId: String, userName: String, bonusPot: Int): Result<Unit> = try {
        firestore.collection("chores").document(choreId)
            .update(
                "assignedUserId", userId,
                "assignedUserName", userName,
                "bonusPot", bonusPot,
                "status", ChoreStatus.ASSIGNED
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteChore(choreId: String): Result<Unit> = try {
        firestore.collection("chores").document(choreId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
