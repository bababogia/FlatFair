package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.Bid
import com.example.flatfair.domain.model.ChoreStatus
import kotlinx.coroutines.flow.Flow

interface ChoreRepository {
    fun getChoresForHousehold(householdId: String): Flow<List<Chore>>
    fun getAssignedChores(userId: String): Flow<List<Chore>>
    fun getBidsForChore(choreId: String): Flow<List<Bid>>
    fun getPendingVerifications(householdId: String): Flow<List<Chore>>
    suspend fun createChore(chore: Chore): Result<Unit>
    suspend fun placeBid(bid: Bid): Result<Unit>
    suspend fun updateChoreStatus(choreId: String, status: ChoreStatus): Result<Unit>
    suspend fun assignChore(choreId: String, userId: String, userName: String, bonusPot: Int): Result<Unit>
    suspend fun deleteChore(choreId: String): Result<Unit>
}
