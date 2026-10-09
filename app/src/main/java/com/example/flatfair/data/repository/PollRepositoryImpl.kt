package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Poll
import com.example.flatfair.domain.repository.PollRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PollRepository {

    override fun getPolls(householdId: String): Flow<List<Poll>> = callbackFlow {
        val subscription = firestore.collection("polls")
            .whereEqualTo("householdId", householdId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val polls = snapshot?.toObjects(Poll::class.java) ?: emptyList()
                trySend(polls)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createPoll(poll: Poll): Result<Unit> = try {
        val docRef = firestore.collection("polls").document()
        val newPoll = poll.copy(id = docRef.id)
        docRef.set(newPoll).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun voteInPoll(pollId: String, userId: String, optionIndex: Int): Result<Unit> = try {
        firestore.collection("polls").document(pollId)
            .update("votes.$userId", optionIndex)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun closePoll(pollId: String): Result<Unit> = try {
        firestore.collection("polls").document(pollId)
            .update("isClosed", true)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deletePoll(pollId: String): Result<Unit> = try {
        firestore.collection("polls").document(pollId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
