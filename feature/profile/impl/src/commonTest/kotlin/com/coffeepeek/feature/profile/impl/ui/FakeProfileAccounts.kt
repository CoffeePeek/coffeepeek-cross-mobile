package com.coffeepeek.feature.profile.impl.ui

import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.domain.ProfileAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal val anna = OwnProfile("Anna", "anna@example.com", "About", "https://img/a.jpg", 3, 2, 1, 4, ProfileAddress("anna", "/users/anna", 1, false))

internal class FakeProfileAccounts(user: String? = "a", own: OwnProfile? = anna) : ProfileAccountPort {
    private val account = MutableStateFlow(user)
    private val cache = MutableStateFlow(own)
    var loads = 0
    var load: suspend () -> Result<OwnProfile> = { Result.failure(IllegalStateException("none")) }
    fun signIn(user: String?) { account.value = user }
    fun publish(value: OwnProfile?) { cache.value = value }
    override fun activeAccount() = account.value
    override fun observeActiveAccount(): Flow<String?> = account
    override fun cachedOwnProfile() = cache.value
    override fun observeOwnProfile(): Flow<OwnProfile?> = cache
    override suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile> {
        if (!forceRefresh) cache.value?.let { return Result.success(it) }
        loads++
        return load().onSuccess { cache.value = it }
    }
}
