package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.model.Household
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TaskStats(
    val pendingCount: Int = 0,
    val assignedCount: Int = 0,
    val completedCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val choreRepository: ChoreRepository,
    private val householdRepository: HouseholdRepository,
    private val expenseRepository: ExpenseRepository,
    private val shoppingRepository: ShoppingRepository,
    private val announcementRepository: AnnouncementRepository,
    private val eventRepository: EventRepository,
    private val pollRepository: PollRepository
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

    val taskStats: StateFlow<TaskStats> = currentHousehold
        .flatMapLatest { household ->
            if (household != null) {
                choreRepository.getChoresForHousehold(household.id)
                    .map { chores ->
                        TaskStats(
                            pendingCount = chores.count { it.status == ChoreStatus.PENDING },
                            assignedCount = chores.count { it.status == ChoreStatus.ASSIGNED || it.status == ChoreStatus.WAITING_FOR_VERIFICATION },
                            completedCount = chores.count { it.status == ChoreStatus.COMPLETED }
                        )
                    }
            } else {
                flowOf(TaskStats())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStats())

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
                        events.count { it.startTime > now && it.startTime < now + 86400000 * 3 }
                    }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activePollsCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                pollRepository.getPolls(user.currentHouseholdId)
                    .map { polls -> polls.count { !it.isClosed && !it.votes.containsKey(user.id) } }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val householdMembers: StateFlow<List<User>> = currentHousehold
        .flatMapLatest { household ->
            if (household != null) {
                authRepository.getHouseholdMembers(household.memberIds)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<User>> = householdMembers
        .map { users -> users.sortedByDescending { it.creditBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
