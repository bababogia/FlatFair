package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getRecentActivities(householdId: String, limit: Int = 10): Flow<List<ActivityLog>>
    suspend fun logActivity(activity: ActivityLog): Result<Unit>
}
