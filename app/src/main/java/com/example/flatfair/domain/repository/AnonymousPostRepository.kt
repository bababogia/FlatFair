package com.example.flatfair.domain.repository

import com.example.flatfair.domain.model.AnonymousPost
import kotlinx.coroutines.flow.Flow

interface AnonymousPostRepository {
    fun getPosts(householdId: String): Flow<List<AnonymousPost>>
    suspend fun createPost(post: AnonymousPost): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
}
