package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Event
import com.example.flatfair.domain.repository.EventRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : EventRepository {

    override fun getEvents(householdId: String): Flow<List<Event>> = callbackFlow {
        val subscription = firestore.collection("events")
            .whereEqualTo("householdId", householdId)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val events = snapshot?.toObjects(Event::class.java) ?: emptyList()
                trySend(events)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addEvent(event: Event): Result<Unit> = try {
        val docRef = firestore.collection("events").document()
        val newEvent = event.copy(id = docRef.id)
        docRef.set(newEvent).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        firestore.collection("events").document(eventId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
