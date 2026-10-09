package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.ActivityLog
import com.example.flatfair.domain.repository.ActivityRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ActivityRepository {

    override fun getRecentActivities(householdId: String, limit: Int): Flow<List<ActivityLog>> = callbackFlow {
        val subscription = firestore.collection("activities")
            .whereEqualTo("householdId", householdId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val activities = snapshot?.toObjects(ActivityLog::class.java) ?: emptyList()
                trySend(activities)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun logActivity(activity: ActivityLog): Result<Unit> = try {
        val docRef = firestore.collection("activities").document()
        val newActivity = activity.copy(id = docRef.id)
        docRef.set(newActivity).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
