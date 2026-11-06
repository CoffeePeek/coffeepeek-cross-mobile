package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.domain.ProfileAddress
import kotlinx.coroutines.flow.flowOf

internal class CommunityProfileAccounts : ProfileAccountPort {
    var load: suspend () -> Result<OwnProfile> = { Result.success(communityOwnProfile("me")) }
    override fun activeAccount(): String? = null
    override fun observeActiveAccount() = flowOf<String?>(null)
    override fun cachedOwnProfile(): OwnProfile? = null
    override fun observeOwnProfile() = flowOf<OwnProfile?>(null)
    override suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile> {
        check(forceRefresh) { "Feed identity must refresh the profile" }
        return load()
    }
}

internal fun communityOwnProfile(slug: String?) = OwnProfile("Me", "", null, null, 0, 0, 0, 0,
    slug?.let { ProfileAddress(it, "/users/$it", 1, false) })
