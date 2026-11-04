package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    /** Synchronous cache read that must belong to the active login lifetime. */
    fun cachedProfile(): UserProfile? = null
    fun observeProfile(): StateFlow<UserProfile?>

    suspend fun refreshProfile(): Result<UserProfile>

    suspend fun getMe(): Result<UserProfile>

    suspend fun getPublicAvatarUrl(userId: String): Result<String?>

    suspend fun requestAccountDeletion(): Result<AccountDeletionRequest>

    suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?>

    suspend fun updateUsername(username: String): Result<Unit>

    suspend fun updateAbout(about: String): Result<Unit>

    suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit>
}
