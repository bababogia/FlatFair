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
class AnonymousBoardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val anonymousPostRepository: AnonymousPostRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val posts: StateFlow<List<AnonymousPost>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                anonymousPostRepository.getPosts(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postMessage(content: String, alias: String) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return

        viewModelScope.launch {
            val authorAlias = if (alias.isNotBlank()) alias else "Mysterious Roommate"
            val post = AnonymousPost(
                householdId = householdId,
                authorId = user.id,
                content = content,
                authorAlias = authorAlias
            )
            val result = anonymousPostRepository.createPost(post)
            if (result.isSuccess) {
                // Log activity anonymously
                activityRepository.logActivity(
                    ActivityLog(
                        householdId = householdId,
                        userId = "anonymous",
                        userName = "Someone",
                        action = "posted anonymously:",
                        targetName = if (content.length > 20) content.take(20) + "..." else content,
                        type = ActivityType.ANONYMOUS_POST
                    )
                )

                // Notify other members
                householdRepository.getHousehold(householdId).firstOrNull()?.let { household ->
                    household.memberIds.filter { it != user.id }.forEach { memberId ->
                        notificationRepository.sendNotification(
                            Notification(
                                userId = memberId,
                                title = "New message on the board",
                                body = "$authorAlias: ${if (content.length > 50) content.take(50) + "..." else content}",
                                type = NotificationType.ANONYMOUS_POST_ADDED
                            )
                        )
                    }
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            anonymousPostRepository.deletePost(postId)
        }
    }
}
