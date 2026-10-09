package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.Household
import com.example.flatfair.domain.model.Transaction
import com.example.flatfair.domain.model.TransactionType
import com.example.flatfair.domain.repository.HouseholdRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : HouseholdRepository {

    override fun getHousehold(id: String): Flow<Household?> = callbackFlow {
        val subscription = firestore.collection("households").document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val household = snapshot?.toObject(Household::class.java)
                trySend(household)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createHousehold(name: String, adminId: String): Result<String> = try {
        val docRef = firestore.collection("households").document()
        val inviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
        
        val household = Household(
            id = docRef.id,
            name = name,
            inviteCode = inviteCode,
            adminIds = listOf(adminId),
            memberIds = listOf(adminId)
        )
        
        firestore.runBatch { batch ->
            batch.set(docRef, household)
            batch.update(firestore.collection("users").document(adminId), "currentHouseholdId", docRef.id)
        }.await()
        
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinHousehold(inviteCode: String, userId: String): Result<Unit> = try {
        val query = firestore.collection("households")
            .whereEqualTo("inviteCode", inviteCode)
            .limit(1)
            .get()
            .await()
            
        if (query.isEmpty) throw Exception("Invalid invite code")
        
        val householdDoc = query.documents[0]
        val householdId = householdDoc.id
        
        firestore.runBatch { batch ->
            batch.update(householdDoc.reference, "memberIds", FieldValue.arrayUnion(userId))
            batch.update(firestore.collection("users").document(userId), "currentHouseholdId", householdId)
        }.await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun leaveHousehold(userId: String, householdId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(firestore.collection("households").document(householdId), "memberIds", FieldValue.arrayRemove(userId))
            batch.update(firestore.collection("users").document(userId), "currentHouseholdId", null)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun distributeStipend(householdId: String, amount: Int): Result<Unit> = try {
        val householdDoc = firestore.collection("households").document(householdId).get().await()
        val household = householdDoc.toObject(Household::class.java) ?: throw Exception("Household not found")
        
        firestore.runBatch { batch ->
            household.memberIds.forEach { userId ->
                // Increment user balance
                batch.update(firestore.collection("users").document(userId), "creditBalance", FieldValue.increment(amount.toLong()))
                
                // Add transaction record
                val transactionRef = firestore.collection("transactions").document()
                val transaction = Transaction(
                    id = transactionRef.id,
                    userId = userId,
                    amount = amount,
                    type = TransactionType.STIPEND,
                    timestamp = System.currentTimeMillis()
                )
                batch.set(transactionRef, transaction)
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
