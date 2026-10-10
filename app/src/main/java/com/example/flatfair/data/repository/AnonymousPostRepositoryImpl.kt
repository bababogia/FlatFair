package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.AnonymousPost
import com.example.flatfair.domain.repository.AnonymousPostRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnonymousPostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AnonymousPostRepository {

    override fun getPosts(householdId: String): Flow<List<AnonymousPost>> = callbackFlow {
        val subscription = firestore.collection("anonymous_posts")
            .whereEqualTo("householdId", householdId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.toObjects(AnonymousPost::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createPost(post: AnonymousPost): Result<Unit> = try {
        val docRef = firestore.collection("anonymous_posts").document()
        val newPost = post.copy(id = docRef.id)
        docRef.set(newPost).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = try {
        firestore.collection("anonymous_posts").document(postId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
