package com.example.flatfair.data.repository

import android.net.Uri
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.Bid
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.model.Comment
import com.example.flatfair.domain.repository.ChoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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

    override fun getCommentsForChore(choreId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection("chores").document(choreId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
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

    override suspend fun addComment(choreId: String, comment: Comment): Result<Unit> = try {
        val docRef = firestore.collection("chores").document(choreId)
            .collection("comments").document()
        val newComment = comment.copy(id = docRef.id)
        docRef.set(newComment).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadProofImage(choreId: String, imageUri: Uri): Result<String> = try {
        val ref = storage.reference.child("chore_proofs/$choreId/${System.currentTimeMillis()}.jpg")
        val uploadTask = ref.putFile(imageUri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateChoreProof(choreId: String, proofImageUrl: String): Result<Unit> = try {
        firestore.collection("chores").document(choreId)
            .update("proofImageUrl", proofImageUrl)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
