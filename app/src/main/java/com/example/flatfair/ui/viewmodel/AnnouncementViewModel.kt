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
class AnnouncementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val announcementRepository: AnnouncementRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val announcements: StateFlow<List<Announcement>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                announcementRepository.getAnnouncements(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postAnnouncement(content: String, isImportant: Boolean) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return

        viewModelScope.launch {
            val announcement = Announcement(
                householdId = householdId,
                authorId = user.id,
                authorName = user.name,
                content = content,
                isImportant = isImportant
            )
            val result = announcementRepository.postAnnouncement(announcement)
            if (result.isSuccess) {
                // Log Activity
                activityRepository.logActivity(
                    ActivityLog(
                        householdId = householdId,
                        userId = user.id,
                        userName = user.name,
                        action = "posted an announcement:",
                        targetName = if (content.length > 20) content.take(20) + "..." else content,
                        type = ActivityType.ANNOUNCEMENT
                    )
                )

                // Send Notifications to other members
                householdRepository.getHousehold(householdId).firstOrNull()?.let { household ->
                    household.memberIds.filter { it != user.id }.forEach { memberId ->
                        notificationRepository.sendNotification(
                            Notification(
                                userId = memberId,
                                title = if (isImportant) "Important Announcement!" else "New Announcement",
                                body = "${user.name}: ${if (content.length > 50) content.take(50) + "..." else content}",
                                type = NotificationType.ANNOUNCEMENT_POSTED
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            announcementRepository.deleteAnnouncement(announcementId)
        }
    }
}
