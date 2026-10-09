package com.example.flatfair.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flatfair.domain.model.*
import com.example.flatfair.domain.repository.ActivityRepository
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.ExpenseRepository
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
    private val expenseRepository: ExpenseRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
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

    fun buyAndCreateExpense(item: ShoppingItem, amount: Double, splitWithUserIds: List<String>) {
        val user = currentUser.value ?: return
        val householdId = user.currentHouseholdId ?: return
        
        viewModelScope.launch {
            // 1. Mark as bought
            shoppingRepository.toggleItemBought(item.id, true)
            
            // 2. Create Expense
            val expense = Expense(
                householdId = householdId,
                title = "Shopping: ${item.name}",
                amount = amount,
                paidByUserId = user.id,
                paidByUserName = user.name,
                splitWithUserIds = splitWithUserIds
            )
            expenseRepository.addExpense(expense)
            
            // 3. Log Activity
            activityRepository.logActivity(ActivityLog(
                householdId = householdId,
                userId = user.id,
                userName = user.name,
                action = "bought and logged expense for:",
                targetName = item.name,
                type = ActivityType.EXPENSE
            ))
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(item.id)
        }
    }
}
