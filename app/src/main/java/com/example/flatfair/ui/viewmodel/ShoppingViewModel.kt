package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.ActivityLog
import com.example.flatfair.domain.model.ActivityType
import com.example.flatfair.domain.model.ShoppingItem
import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.ActivityRepository
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val shoppingRepository: ShoppingRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shoppingItems: StateFlow<List<ShoppingItem>> = currentUser
        .flatMapLatest { user ->
            if (user?.currentHouseholdId != null) {
                shoppingRepository.getShoppingItems(user.currentHouseholdId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String, quantity: String, isUrgent: Boolean) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return
        
        viewModelScope.launch {
            val item = ShoppingItem(
                householdId = householdId,
                name = name,
                quantity = quantity,
                addedByUserId = user.id,
                addedByUserName = user.name,
                isUrgent = isUrgent
            )
            val result = shoppingRepository.addShoppingItem(item)
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = householdId,
                    userId = user.id,
                    userName = user.name,
                    action = "added to shopping list:",
                    targetName = name,
                    type = ActivityType.SHOPPING
                ))
            }
        }
    }

    fun toggleItemBought(item: ShoppingItem) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val nextState = !item.isBought
            val result = shoppingRepository.toggleItemBought(item.id, nextState)
            if (result.isSuccess && nextState) {
                activityRepository.logActivity(ActivityLog(
                    householdId = user.currentHouseholdId ?: "",
                    userId = user.id,
                    userName = user.name,
                    action = "bought:",
                    targetName = item.name,
                    type = ActivityType.SHOPPING
                ))
            }
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(item.id)
        }
    }
}
