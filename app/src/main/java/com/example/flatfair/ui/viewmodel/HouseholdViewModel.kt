package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.Household
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentHousehold: StateFlow<Household?> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                householdRepository.getHousehold(user.currentHouseholdId)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val householdMembers: StateFlow<List<User>> = currentHousehold
        .flatMapLatest { household ->
            if (household != null) {
                authRepository.getHouseholdMembers(household.memberIds)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAdmin: StateFlow<Boolean> = combine(currentUser, currentHousehold) { user, household ->
        household?.adminIds?.contains(user?.id) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow<HouseholdUiState>(HouseholdUiState.Idle)
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    fun createHousehold(name: String) {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            _uiState.value = HouseholdUiState.Loading
            val result = householdRepository.createHousehold(name, userId)
            if (result.isSuccess) {
                _uiState.value = HouseholdUiState.Success
            } else {
                _uiState.value = HouseholdUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create household")
            }
        }
    }

    fun joinHousehold(inviteCode: String) {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            _uiState.value = HouseholdUiState.Loading
            val result = householdRepository.joinHousehold(inviteCode, userId)
            if (result.isSuccess) {
                _uiState.value = HouseholdUiState.Success
            } else {
                _uiState.value = HouseholdUiState.Error(result.exceptionOrNull()?.message ?: "Failed to join household")
            }
        }
    }

    fun leaveHousehold() {
        val userId = currentUser.value?.id ?: return
        val householdId = currentUser.value?.currentHouseholdId ?: return
        viewModelScope.launch {
            _uiState.value = HouseholdUiState.Loading
            val result = householdRepository.leaveHousehold(userId, householdId)
            if (result.isSuccess) {
                _uiState.value = HouseholdUiState.Success
            } else {
                _uiState.value = HouseholdUiState.Error(result.exceptionOrNull()?.message ?: "Failed to leave household")
            }
        }
    }

    fun distributeStipend(amount: Int) {
        val householdId = currentHousehold.value?.id ?: return
        viewModelScope.launch {
            _uiState.value = HouseholdUiState.Loading
            val result = householdRepository.distributeStipend(householdId, amount)
            if (result.isSuccess) {
                _uiState.value = HouseholdUiState.Idle
            } else {
                _uiState.value = HouseholdUiState.Error(result.exceptionOrNull()?.message ?: "Failed to distribute stipend")
            }
        }
    }

    fun updateHouseRules(rules: String) {
        val householdId = currentHousehold.value?.id ?: return
        viewModelScope.launch {
            _uiState.value = HouseholdUiState.Loading
            val result = householdRepository.updateHouseRules(householdId, rules)
            if (result.isSuccess) {
                _uiState.value = HouseholdUiState.Idle
            } else {
                _uiState.value = HouseholdUiState.Error(result.exceptionOrNull()?.message ?: "Failed to update house rules")
            }
        }
    }

    fun clearError() {
        _uiState.value = HouseholdUiState.Idle
    }
}

sealed class HouseholdUiState {
    data object Idle : HouseholdUiState()
    data object Loading : HouseholdUiState()
    data object Success : HouseholdUiState()
    data class Error(val message: String) : HouseholdUiState()
}
