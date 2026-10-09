package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUser(userId: String): Flow<User?> = callbackFlow {
        val subscription = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateCredits(userId: String, amountChange: Int): Result<Unit> = try {
        firestore.collection("users").document(userId)
            .update("creditBalance", FieldValue.increment(amountChange.toLong()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProfile(userId: String, name: String, profilePicUrl: String?): Result<Unit> = try {
        firestore.collection("users").document(userId)
            .update(
                "name", name,
                "profilePicUrl", profilePicUrl
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
