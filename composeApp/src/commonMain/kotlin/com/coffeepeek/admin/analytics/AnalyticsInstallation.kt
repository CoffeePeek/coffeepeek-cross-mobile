package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal const val ANALYTICS_CONSENT_KEY = "coffeepeek.analyticsConsent.v1"

internal fun installAnalytics(settings: SettingRepository, sessions: SessionReader, gateway: ApiClients) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val service = createAnalyticsService(settings, scope)
    Analytics.install(service)
    gateway.requestObserver = analyticsRequestObserver(service, sessions)
    com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.current =
        com.coffeepeek.admin.feature.favorites.ui.TrackedFavoriteInteractions(sessions, scope)
    scope.launch { observeAnalyticsSession(service, sessions) }
}

private fun createAnalyticsService(settings: SettingRepository, scope: CoroutineScope) = AnalyticsService(
    AnalyticsContext(
        app = "coffee-peek-${AppConfig.updatePlatform}",
        version = runCatching { "${AppConfig.versionName}+${AppConfig.versionCode ?: 0}" }.getOrDefault("unknown"),
        environment = if (AppConfig.isDebug) "development" else "production",
    ),
    settingsConsent(settings),
    PostHogTransport(),
    scope,
    enabled = postHogProjectToken.isNotBlank() && (!AppConfig.isDebug || analyticsDebugEnabled),
)

private fun settingsConsent(settings: SettingRepository) = object : AnalyticsConsentStore {
    override suspend fun read() = when (settings.read(ANALYTICS_CONSENT_KEY)?.value) {
        "accepted" -> AnalyticsConsent.Accepted
        "declined" -> AnalyticsConsent.Declined
        else -> AnalyticsConsent.Unknown
    }

    override suspend fun write(consent: AnalyticsConsent) {
        settings.save(Setting(ANALYTICS_CONSENT_KEY, if (consent == AnalyticsConsent.Accepted) "accepted" else "declined"))
    }
}
