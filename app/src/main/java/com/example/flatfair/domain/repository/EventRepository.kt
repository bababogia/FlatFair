package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(householdId: String): Flow<List<Event>>
    suspend fun addEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
