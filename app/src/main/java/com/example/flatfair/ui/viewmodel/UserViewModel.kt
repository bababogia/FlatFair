package com.example.flatfair.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.TransactionType
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.TransactionRepository
import com.example.flatfair.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserStats(
    val choresCompleted: Int = 0,
    val totalCreditsEarned: Int = 0,
    val totalCreditsSpent: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userStats: StateFlow<UserStats> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                getStatsForUser(user.id)
            } else {
                flowOf(UserStats())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    fun getStatsForUser(userId: String): Flow<UserStats> {
        return transactionRepository.getTransactionsForUser(userId)
            .map { transactions ->
                UserStats(
                    choresCompleted = transactions.count { it.type == TransactionType.TASK_REWARD },
                    totalCreditsEarned = transactions.filter { it.type == TransactionType.TASK_REWARD || it.type == TransactionType.STIPEND }.sumOf { it.amount },
                    totalCreditsSpent = transactions.filter { it.type == TransactionType.BID_SPENT }.sumOf { it.amount }
                )
            }
    }

    fun getUserById(userId: String): Flow<User?> {
        return userRepository.getUser(userId)
    }

    fun updateProfile(name: String, imageUri: Uri?) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            val user = authRepository.currentUser.firstOrNull()
            if (user != null) {
                var profilePicUrl: String? = user.profilePicUrl
                
                if (imageUri != null) {
                    val uploadResult = userRepository.uploadProfilePicture(user.id, imageUri)
                    if (uploadResult.isSuccess) {
                        profilePicUrl = uploadResult.getOrThrow()
                    } else {
                        _uiState.value = UserUiState.Error(uploadResult.exceptionOrNull()?.message ?: "Image upload failed")
                        return@launch
                    }
                }

                val result = userRepository.updateProfile(user.id, name, profilePicUrl)
                if (result.isSuccess) {
                    _uiState.value = UserUiState.Success
                } else {
                    _uiState.value = UserUiState.Error(result.exceptionOrNull()?.message ?: "Update failed")
                }
            } else {
                _uiState.value = UserUiState.Error("User not found")
            }
        }
    }

    fun clearState() {
        _uiState.value = UserUiState.Idle
    }
}

sealed class UserUiState {
    data object Idle : UserUiState()
    data object Loading : UserUiState()
    data object Success : UserUiState()
    data class Error(val message: String) : UserUiState()
}
