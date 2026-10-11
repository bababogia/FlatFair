package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Poll
import kotlinx.coroutines.flow.Flow

interface PollRepository {
    fun getPolls(householdId: String): Flow<List<Poll>>
    suspend fun createPoll(poll: Poll): Result<Unit>
    suspend fun updatePoll(poll: Poll): Result<Unit>
    suspend fun voteInPoll(pollId: String, userId: String, optionIndex: Int): Result<Unit>
    suspend fun closePoll(pollId: String): Result<Unit>
    suspend fun deletePoll(pollId: String): Result<Unit>
}
