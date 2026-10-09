package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.model.Household
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.AnnouncementRepository
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.ChoreRepository
import com.example.flatfair.domain.repository.EventRepository
import com.example.flatfair.domain.repository.ExpenseRepository
import com.example.flatfair.domain.repository.HouseholdRepository
import com.example.flatfair.domain.repository.ShoppingRepository
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
    private val expenseRepository: ExpenseRepository,
    private val shoppingRepository: ShoppingRepository,
    private val announcementRepository: AnnouncementRepository,
    private val eventRepository: EventRepository
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

    val pendingShoppingItemsCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                shoppingRepository.getShoppingItems(user.currentHouseholdId)
                    .map { items -> items.count { !it.isBought } }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val announcementsCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                announcementRepository.getAnnouncements(user.currentHouseholdId).map { it.size }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upcomingEventsCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                eventRepository.getEvents(user.currentHouseholdId)
                    .map { events ->
                        val now = System.currentTimeMillis()
                        events.count { it.startTime > now && it.startTime < now + 86400000 * 3 } // Next 3 days
                    }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val leaderboard: StateFlow<List<User>> = currentHousehold
        .flatMapLatest { household ->
            if (household != null) {
                authRepository.getHouseholdMembers(household.memberIds)
            } else {
                flowOf(emptyList())
            }
        }
        .map { users -> users.sortedByDescending { it.creditBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
