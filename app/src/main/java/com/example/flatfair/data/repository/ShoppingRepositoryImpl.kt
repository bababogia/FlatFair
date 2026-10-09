package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.ShoppingItem
import com.example.flatfair.domain.repository.ShoppingRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ShoppingRepository {

    override fun getShoppingItems(householdId: String): Flow<List<ShoppingItem>> = callbackFlow {
        val subscription = firestore.collection("shopping_items")
            .whereEqualTo("householdId", householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val items = snapshot?.toObjects(ShoppingItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addShoppingItem(item: ShoppingItem): Result<Unit> = try {
        val docRef = firestore.collection("shopping_items").document()
        val newItem = item.copy(id = docRef.id)
        docRef.set(newItem).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleItemBought(itemId: String, isBought: Boolean): Result<Unit> = try {
        firestore.collection("shopping_items").document(itemId)
            .update("isBought", isBought)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> = try {
        firestore.collection("shopping_items").document(itemId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
