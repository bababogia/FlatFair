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
class ShoppingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val shoppingRepository: ShoppingRepository,
    private val expenseRepository: ExpenseRepository,
    private val activityRepository: ActivityRepository,
    private val notificationRepository: NotificationRepository,
    private val householdRepository: HouseholdRepository
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
                    targetName = if (isUrgent) "URGENT: $name" else name,
                    type = ActivityType.SHOPPING
                ))

                // Notify household members
                householdRepository.getHousehold(householdId).firstOrNull()?.let { household ->
                    household.memberIds.filter { it != user.id }.forEach { memberId ->
                        notificationRepository.sendNotification(Notification(
                            userId = memberId,
                            title = if (isUrgent) "Urgent Shopping Needed!" else "New Shopping Item",
                            body = "${user.name} added $name to the list.",
                            type = NotificationType.NUDGE
                        ))
                    }
                }
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
            shoppingRepository.toggleItemBought(item.id, true)
            
            val expense = Expense(
                householdId = householdId,
                title = "Shopping: ${item.name}",
                amount = amount,
                paidByUserId = user.id,
                paidByUserName = user.name,
                splitWithUserIds = splitWithUserIds
            )
            val result = expenseRepository.addExpense(expense)
            
            if (result.isSuccess) {
                activityRepository.logActivity(ActivityLog(
                    householdId = householdId,
                    userId = user.id,
                    userName = user.name,
                    action = "bought and logged expense for:",
                    targetName = item.name,
                    type = ActivityType.EXPENSE
                ))

                // Notify split participants
                splitWithUserIds.forEach { participantId ->
                    notificationRepository.sendNotification(Notification(
                        userId = participantId,
                        title = "Shopping Expense Added",
                        body = "${user.name} bought ${item.name}. You owe your share of $$amount.",
                        type = NotificationType.EXPENSE_ADDED
                    ))
                }
            }
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(item.id)
        }
    }
}
