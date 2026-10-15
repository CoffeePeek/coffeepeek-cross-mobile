package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

internal const val ANALYTICS_CONSENT_KEY = "coffeepeek.analyticsConsent.v1"

internal fun installAnalytics(settings: SettingRepository, sessions: SessionRepository, gateway: com.coffeepeek.api.CoffeePeekClient) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val service = AnalyticsService(
        AnalyticsContext(
            app = "coffee-peek-${AppConfig.updatePlatform}",
            version = runCatching { "${AppConfig.versionName}+${AppConfig.versionCode ?: 0}" }.getOrDefault("unknown"),
            environment = if (AppConfig.isDebug) "development" else "production",
        ),
        object : AnalyticsConsentStore {
            override suspend fun read() = when (settings.read(ANALYTICS_CONSENT_KEY)?.value) {
                "accepted" -> AnalyticsConsent.Accepted
                "declined" -> AnalyticsConsent.Declined
                else -> AnalyticsConsent.Unknown
            }
            override suspend fun write(consent: AnalyticsConsent) {
                settings.save(Setting(ANALYTICS_CONSENT_KEY, if (consent == AnalyticsConsent.Accepted) "accepted" else "declined"))
            }
        },
        PostHogTransport(), scope,
        enabled = !AppConfig.isDebug || analyticsDebugEnabled,
    )
    Analytics.install(service)
    gateway.requestObserver =
        com.coffeepeek.api.RequestObserver { category, method ->
            val operation = service.operationScope()
            val account = sessions.peekSession()?.userId
            com.coffeepeek.api.RequestObservation { status, error, duration, retried ->
                if (sessions.peekSession()?.userId == account) {
                    val failure = status?.let { safeStatusError(it) } ?: error?.let(::safeAnalyticsError)
                    if (failure?.category != "cancelled") service.track(AnalyticsEvent.API_REQUEST_COMPLETED,
                        mapOf("category" to category, "method" to method, "outcome" to if (error == null && (status ?: 0) in 100..399) "success" else "failure",
                            "duration_ms" to duration, "auth_retried" to retried) +
                            status?.takeIf { it in 100..599 }?.let { mapOf("status_code" to it) }.orEmpty() +
                            failure?.let { mapOf("error_category" to it.category) }.orEmpty(), operation)
                }
            }
        }
    com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.current =
        com.coffeepeek.admin.feature.favorites.ui.TrackedFavoriteInteractions(sessions, scope)
    scope.launch {
        try {
            service.restoreConsent()
            sessions.warmCache()
            sessions.observeSession().catch { service.syncIdentityNow(null, authRestoring = true) }.collect { session ->
                // Never fall back to JWT/access token or a public profile slug for identity.
                val active = sessions.isActiveSession(session)
                service.syncIdentity(session?.userId?.takeIf { active }, authRestoring = active && session?.userId == null)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            service.syncIdentityNow(null, authRestoring = true)
        }
    }
}
