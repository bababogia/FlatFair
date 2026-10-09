package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Announcement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {
    fun getAnnouncements(householdId: String): Flow<List<Announcement>>
    suspend fun postAnnouncement(announcement: Announcement): Result<Unit>
    suspend fun deleteAnnouncement(announcementId: String): Result<Unit>
}
