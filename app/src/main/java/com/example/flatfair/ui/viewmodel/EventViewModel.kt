package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val events: StateFlow<List<Event>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                eventRepository.getEvents(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingEvents: StateFlow<List<Event>> = events
        .map { eventList ->
            val now = System.currentTimeMillis()
            eventList.filter { it.endTime > now }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(title: String, description: String, startTime: Long, endTime: Long, type: EventType) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return

        viewModelScope.launch {
            val event = Event(
                householdId = householdId,
                title = title,
                description = description,
                startTime = startTime,
                endTime = endTime,
                createdByUserId = user.id,
                createdByUserName = user.name,
                type = type
            )
            val result = eventRepository.addEvent(event)
            if (result.isSuccess) {
                // Log Activity
                activityRepository.logActivity(
                    ActivityLog(
                        householdId = householdId,
                        userId = user.id,
                        userName = user.name,
                        action = "added an event:",
                        targetName = title,
                        type = ActivityType.EVENT
                    )
                )

                // Notify other members
                householdRepository.getHousehold(householdId).firstOrNull()?.let { household ->
                    household.memberIds.filter { it != user.id }.forEach { memberId ->
                        notificationRepository.sendNotification(
                            Notification(
                                userId = memberId,
                                title = "New Household Event",
                                body = "${user.name} added: $title",
                                type = NotificationType.EVENT_ADDED,
                                relatedId = event.id
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }
}
