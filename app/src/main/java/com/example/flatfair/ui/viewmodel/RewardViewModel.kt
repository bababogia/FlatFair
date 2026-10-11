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
class RewardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rewardRepository: RewardRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val activityRepository: ActivityRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentHousehold = currentUser.flatMapLatest { user ->
        if (user?.currentHouseholdId != null) {
            householdRepository.getHousehold(user.currentHouseholdId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAdmin = combine(currentUser, currentHousehold) { user, household ->
        household?.adminIds?.contains(user?.id) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val rewards: StateFlow<List<Reward>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                rewardRepository.getRewards(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReward(title: String, description: String, cost: Int) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return
        
        viewModelScope.launch {
            val reward = Reward(
                householdId = householdId,
                title = title,
                description = description,
                cost = cost,
                createdByUserId = user.id
            )
            val result = rewardRepository.addReward(reward)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = householdId,
                    userId = user.id,
                    userName = user.name,
                    action = "added a new reward:",
                    targetName = title,
                    type = ActivityType.REWARD
                ))
            }
        }
    }

    fun updateReward(reward: Reward) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = rewardRepository.updateReward(reward)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "updated the reward:",
                    targetName = reward.title,
                    type = ActivityType.REWARD
                ))
            }
        }
    }

    fun redeemReward(reward: Reward) {
        val user = currentUser.value ?: return
        if (user.creditBalance < reward.cost) return

        viewModelScope.launch {
            val result = rewardRepository.redeemReward(
                userId = user.id,
                reward = reward,
                transaction = Transaction(
                    userId = user.id,
                    amount = reward.cost,
                    type = TransactionType.REWARD_REDEMPTION,
                    rewardId = reward.id
                ),
                activityLog = ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "redeemed the reward:",
                    targetName = reward.title,
                    type = ActivityType.REWARD
                )
            )
        }
    }

    fun deleteReward(rewardId: String, title: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = rewardRepository.deleteReward(rewardId)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "removed the reward:",
                    targetName = title,
                    type = ActivityType.REWARD
                ))
            }
        }
    }

    fun toggleAvailability(rewardId: String, isAvailable: Boolean) {
        viewModelScope.launch {
            rewardRepository.toggleRewardAvailability(rewardId, isAvailable)
        }
    }
}
