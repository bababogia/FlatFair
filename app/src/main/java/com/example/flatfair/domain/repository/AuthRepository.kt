package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, name: String, password: String): Result<Unit>
    suspend fun logout()
    fun isUserLoggedIn(): Boolean
}
