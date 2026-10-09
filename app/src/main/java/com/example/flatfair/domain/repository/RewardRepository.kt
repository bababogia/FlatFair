package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.Reward
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun getRewards(householdId: String): Flow<List<Reward>>
    suspend fun addReward(reward: Reward): Result<Unit>
    suspend fun deleteReward(rewardId: String): Result<Unit>
    suspend fun toggleRewardAvailability(rewardId: String, isAvailable: Boolean): Result<Unit>
}
