package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Household
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    fun getHousehold(id: String): Flow<Household?>
    suspend fun createHousehold(name: String, adminId: String): Result<String>
    suspend fun joinHousehold(inviteCode: String, userId: String): Result<Unit>
    suspend fun leaveHousehold(userId: String, householdId: String): Result<Unit>
    suspend fun distributeStipend(householdId: String, amount: Int): Result<Unit>
}
