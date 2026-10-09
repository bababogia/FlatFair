package com.example.flatfair.domain.repository

import android.net.Uri
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.Bid
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface ChoreRepository {
    fun getChoresForHousehold(householdId: String): Flow<List<Chore>>
    fun getAssignedChores(userId: String): Flow<List<Chore>>
    fun getBidsForChore(choreId: String): Flow<List<Bid>>
    fun getPendingVerifications(householdId: String): Flow<List<Chore>>
    fun getCommentsForChore(choreId: String): Flow<List<Comment>>
    suspend fun createChore(chore: Chore): Result<Unit>
    suspend fun placeBid(bid: Bid): Result<Unit>
    suspend fun updateChoreStatus(choreId: String, status: ChoreStatus): Result<Unit>
    suspend fun assignChore(choreId: String, userId: String, userName: String, bonusPot: Int): Result<Unit>
    suspend fun deleteChore(choreId: String): Result<Unit>
    suspend fun addComment(choreId: String, comment: Comment): Result<Unit>
    suspend fun uploadProofImage(choreId: String, imageUri: Uri): Result<String>
    suspend fun updateChoreProof(choreId: String, proofImageUrl: String): Result<Unit>
}
