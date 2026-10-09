package com.example.flatfair.domain.repository

import android.net.Uri
import com.example.flatfair.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(userId: String): Flow<User?>
    suspend fun updateCredits(userId: String, amountChange: Int): Result<Unit>
    suspend fun updateProfile(userId: String, name: String, profilePicUrl: String?): Result<Unit>
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String>
}
