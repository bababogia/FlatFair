package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Announcement
import com.example.flatfair.domain.repository.AnnouncementRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AnnouncementRepository {

    override fun getAnnouncements(householdId: String): Flow<List<Announcement>> = callbackFlow {
        val subscription = firestore.collection("announcements")
            .whereEqualTo("householdId", householdId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val announcements = snapshot?.toObjects(Announcement::class.java) ?: emptyList()
                trySend(announcements)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun postAnnouncement(announcement: Announcement): Result<Unit> = try {
        val docRef = firestore.collection("announcements").document()
        val newAnnouncement = announcement.copy(id = docRef.id)
        docRef.set(newAnnouncement).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAnnouncement(announcementId: String): Result<Unit> = try {
        firestore.collection("announcements").document(announcementId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
