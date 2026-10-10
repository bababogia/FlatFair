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
class PollViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pollRepository: PollRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val polls: StateFlow<List<Poll>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                pollRepository.getPolls(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPoll(question: String, options: List<String>) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return

        viewModelScope.launch {
            val poll = Poll(
                householdId = householdId,
                question = question,
                options = options,
                createdByUserId = user.id,
                createdByUserName = user.name
            )
            val result = pollRepository.createPoll(poll)
            if (result.isSuccess) {
                // Log Activity
                activityRepository.logActivity(
                    ActivityLog(
                        householdId = householdId,
                        userId = user.id,
                        userName = user.name,
                        action = "started a new poll:",
                        targetName = question,
                        type = ActivityType.POLL
                    )
                )

                // Notify household members
                householdRepository.getHousehold(householdId).firstOrNull()?.let { household ->
                    household.memberIds.filter { it != user.id }.forEach { memberId ->
                        notificationRepository.sendNotification(
                            Notification(
                                userId = memberId,
                                title = "New Household Poll",
                                body = "${user.name} asks: $question",
                                type = NotificationType.POLL_STARTED,
                                relatedId = poll.id
                            )
                        )
                    }
                }
            }
        }
    }

    fun vote(pollId: String, optionIndex: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            pollRepository.voteInPoll(pollId, user.id, optionIndex)
        }
    }

    fun closePoll(pollId: String, question: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = pollRepository.closePoll(pollId)
            if (result.isSuccess) {
                activityRepository.logActivity(
                    ActivityLog(
                        householdId = user.currentHouseholdId ?: "",
                        userId = user.id,
                        userName = user.name,
                        action = "closed the poll:",
                        targetName = question,
                        type = ActivityType.POLL
                    )
                )
            }
        }
    }

    fun deletePoll(pollId: String) {
        viewModelScope.launch {
            pollRepository.deletePoll(pollId)
        }
    }
}
