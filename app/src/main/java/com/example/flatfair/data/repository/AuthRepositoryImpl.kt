package com.example.flatfair.data.repository

import com.example.flatfair.domain.model.User
import com.example.flatfair.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        var firestoreListener: ListenerRegistration? = null
        
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            firestoreListener?.remove()
            
            if (firebaseUser == null) {
                trySend(null)
            } else {
                firestoreListener = firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        val user = snapshot?.toObject(User::class.java)
                        trySend(user)
                    }
            }
        }
        
        firebaseAuth.addAuthStateListener(authListener)
        awaitClose { 
            firebaseAuth.removeAuthStateListener(authListener)
            firestoreListener?.remove()
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> = try {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun register(email: String, name: String, password: String): Result<Unit> = try {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("User creation failed")
        
        val newUser = User(
            id = firebaseUser.uid,
            name = name,
            email = email,
            creditBalance = 100
        )
        
        firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override fun getHouseholdMembers(memberIds: List<String>): Flow<List<User>> = callbackFlow {
        if (memberIds.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val subscription = firestore.collection("users")
            .whereIn(FieldPath.documentId(), memberIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                trySend(users)
            }
        awaitClose { subscription.remove() }
    }
}
