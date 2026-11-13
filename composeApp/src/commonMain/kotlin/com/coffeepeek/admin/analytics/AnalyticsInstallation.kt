package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.data.createAnalyticsPreferences
import com.coffeepeek.feature.settings.domain.AnalyticsPreferences
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal fun installAnalytics(settings: SettingRepository, sessions: SessionReader, gateway: ApiClients) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val service = createAnalyticsService(createAnalyticsPreferences(gateway.client, sessions, settings), scope)
    Analytics.install(service)
    gateway.requestObserver = analyticsRequestObserver(service, sessions)
    com.coffeepeek.admin.di.favorites.FavoriteInteractions.current =
        com.coffeepeek.admin.di.favorites.TrackedFavoriteInteractions(sessions, scope)
    scope.launch { observeAnalyticsSession(service, sessions) }
}

private fun createAnalyticsService(preferences: AnalyticsPreferences, scope: CoroutineScope) = AnalyticsService(
    AnalyticsContext(
        app = "coffee-peek-${AppConfig.updatePlatform}",
        version = runCatching { "${AppConfig.versionName}+${AppConfig.versionCode ?: 0}" }.getOrDefault("unknown"),
        environment = if (AppConfig.isDebug) "development" else "production",
    ),
    accountConsent(preferences),
    PostHogTransport(),
    scope,
    enabled = postHogProjectToken.isNotBlank() && (!AppConfig.isDebug || analyticsDebugEnabled),
)

private fun accountConsent(preferences: AnalyticsPreferences) = object : AnalyticsConsentStore {
    override suspend fun read(userId: String?): AnalyticsConsent {
        preferences.prepare().getOrThrow()
        if (userId == null) return AnalyticsConsent.Unknown
        return if (preferences.load(userId).getOrThrow().enabled) AnalyticsConsent.Accepted else AnalyticsConsent.Declined
    }

    override suspend fun write(userId: String?, consent: AnalyticsConsent) {
        if (userId != null) preferences.set(userId, consent == AnalyticsConsent.Accepted).getOrThrow()
    }
}
