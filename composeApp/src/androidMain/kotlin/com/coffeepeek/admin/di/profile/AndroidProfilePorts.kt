package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAnalyticsPort
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import com.coffeepeek.feature.profile.domain.ProfileAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Adapter over the single session and own-profile cache, so the migrated screens cannot diverge from legacy ones. */
internal class AndroidProfileAccountPort(
    private val sessions: SessionRepository,
    private val users: UserRepository,
) : ProfileAccountPort {
    override fun activeAccount(): String? = key(sessions.peekSession())

    override fun observeActiveAccount(): Flow<String?> = sessions.observeSession().map(::key).distinctUntilChanged()

    override fun cachedOwnProfile(): OwnProfile? = users.observeProfile().value?.toOwnProfile()

    override fun observeOwnProfile(): Flow<OwnProfile?> = users.observeProfile().map { it?.toOwnProfile() }

    override suspend fun loadOwnProfile(forceRefresh: Boolean): Result<OwnProfile> {
        if (!forceRefresh) cachedOwnProfile()?.let { return Result.success(it) }
        return (if (forceRefresh) users.refreshProfile() else users.getMe()).map(UserProfile::toOwnProfile)
    }

    private fun key(session: com.coffeepeek.domain.model.Session?): String? =
        session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
}

internal fun UserProfile.toOwnProfile() = OwnProfile(userName, email, about, avatarUrl, checkInCount, addedShopsCount,
    addedRoastersCount, followersCount, address?.toProfileAddress())

internal fun PublicAddress.toProfileAddress() = ProfileAddress(slug, canonicalPath, revision, isAlias)

internal class AndroidProfileAuthorVisibility(private val source: AuthorVisibility) : ProfileAuthorVisibility {
    override fun current(): ProfileAuthorVisibilityState = source.visibility.value.toState()
    override fun observe(): Flow<ProfileAuthorVisibilityState> = source.visibility.map { it.toState() }
    override suspend fun refresh(): Result<Unit> = source.refresh()
}

private fun AuthorVisibilitySnapshot.toState() = ProfileAuthorVisibilityState(revision, ready, blockedIds, blockedSlugs)

internal object AndroidProfileAnalytics : ProfileAnalyticsPort {
    override fun favoritesOpened() {
        Analytics.track(AnalyticsEvent.FAVORITES_OPENED, "entry_point" to "profile")
    }
}
