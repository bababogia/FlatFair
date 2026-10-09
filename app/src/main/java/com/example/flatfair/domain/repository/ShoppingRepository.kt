package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    fun getShoppingItems(householdId: String): Flow<List<ShoppingItem>>
    suspend fun addShoppingItem(item: ShoppingItem): Result<Unit>
    suspend fun toggleItemBought(itemId: String, isBought: Boolean): Result<Unit>
    suspend fun deleteItem(itemId: String): Result<Unit>
}
