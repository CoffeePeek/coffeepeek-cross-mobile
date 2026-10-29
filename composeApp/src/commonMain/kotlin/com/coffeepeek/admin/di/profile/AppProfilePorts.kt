package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.profile.api.ProfileAnalyticsPort
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AppProfileAuthorVisibility(private val source: AuthorVisibility) : ProfileAuthorVisibility {
    override fun current(): ProfileAuthorVisibilityState = source.visibility.value.toState()
    override fun observe(): Flow<ProfileAuthorVisibilityState> = source.visibility.map { it.toState() }
    override suspend fun refresh(): Result<Unit> = source.refresh()
}

private fun AuthorVisibilitySnapshot.toState() = ProfileAuthorVisibilityState(revision, ready, blockedIds, blockedSlugs)

internal object AppProfileAnalytics : ProfileAnalyticsPort {
    override fun favoritesOpened() {
        Analytics.track(AnalyticsEvent.FAVORITES_OPENED, "entry_point" to "profile")
    }
}
