package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Notification
import com.example.flatfair.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotificationsForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        val subscription = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                trySend(notifications)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = try {
        firestore.collection("notifications").document(notificationId)
            .update("isRead", true)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = try {
        firestore.collection("notifications").document(notificationId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendNotification(notification: Notification): Result<Unit> = try {
        val docRef = firestore.collection("notifications").document()
        val newNotification = notification.copy(id = docRef.id)
        docRef.set(newNotification).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
