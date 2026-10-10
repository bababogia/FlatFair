package com.example.flatfair.data.repository

import android.net.Uri
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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
        val updates = mutableMapOf<String, Any>(
            "name" to name
        )
        profilePicUrl?.let { updates["profilePicUrl"] = it }
        
        firestore.collection("users").document(userId)
            .update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> = try {
        val ref = storage.reference.child("profile_pictures/$userId.jpg")
        ref.putFile(imageUri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePresence(userId: String, isHome: Boolean): Result<Unit> = try {
        firestore.collection("users").document(userId)
            .update(
                "isHome", isHome,
                "lastActive", System.currentTimeMillis()
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
