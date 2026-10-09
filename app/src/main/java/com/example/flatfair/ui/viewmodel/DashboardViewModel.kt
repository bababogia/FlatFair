package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.ChoreRepository
import com.example.flatfair.domain.repository.ExpenseRepository
import com.example.flatfair.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val choreRepository: ChoreRepository,
    private val householdRepository: HouseholdRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeChores: StateFlow<List<Chore>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                choreRepository.getAssignedChores(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingExpensesCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                expenseRepository.getExpensesForHousehold(user.currentHouseholdId)
                    .map { expenses -> 
                        expenses.count { !it.isSettled && (it.paidByUserId == user.id || it.splitWithUserIds.contains(user.id)) }
                    }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingVerificationsCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                householdRepository.getHousehold(user.currentHouseholdId)
                    .flatMapLatest { household ->
                        if (household?.adminIds?.contains(user.id) == true) {
                            choreRepository.getPendingVerifications(household.id).map { it.size }
                        } else {
                            flowOf(0)
                        }
                    }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableBidsCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                choreRepository.getChoresForHousehold(user.currentHouseholdId)
                    .map { chores -> chores.count { it.status == ChoreStatus.PENDING } }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val leaderboard: StateFlow<List<User>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                householdRepository.getHousehold(user.currentHouseholdId)
                    .flatMapLatest { household ->
                        if (household != null) {
                            authRepository.getHouseholdMembers(household.memberIds)
                        } else {
                            flowOf(emptyList())
                        }
                    }
            } else {
                flowOf(emptyList())
            }
        }
        .map { users -> users.sortedByDescending { it.creditBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
