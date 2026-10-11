package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Consent-gated product events shared by Android and iOS.
 *
 * The backend alone records confirmed favorites and published check-ins.
 */
object ProductAnalytics {
    private const val CONSENT_KEY = "analytics_consent"
    private const val GRANTED = "granted"
    private const val DENIED = "denied"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableConsentEnabled = MutableStateFlow(false)
    val consentEnabled: StateFlow<Boolean> = mutableConsentEnabled

    private var settingsRepository: SettingRepository? = null
    private var started = false
    private var identifiedUserId: String? = null

    val isConfigured: Boolean
        get() = started

    fun start(
        context: PostHogContext,
        settings: SettingRepository,
        sessions: SessionRepository,
    ) {
        if (started) return
        val token = AppConfig.postHogProjectToken.trim()
        if (!token.startsWith("phc_")) return

        PostHog.setup(
            PostHogConfig(
                projectToken = token,
                host = PostHogConfig.HOST_EU,
                optOut = true,
                captureApplicationLifecycleEvents = false,
                captureScreenViews = false,
                captureDeepLinks = false,
                autocapture = false,
                preloadFeatureFlags = false,
                sendFeatureFlagEvent = false,
                sessionRecording = null,
            ),
            context,
        )
        settingsRepository = settings
        started = true

        scope.launch {
            combine(
                settings.readFlow(CONSENT_KEY),
                sessions.observeSession(),
            ) { savedConsent, session ->
                val granted = savedConsent?.value == GRANTED
                val userId = session
                    ?.takeIf(sessions::isActiveSession)
                    ?.userId
                    ?.lowercase()
                    ?.takeIf { it.isNotBlank() }
                granted to userId
            }
                .distinctUntilChanged()
                .collect { (granted, userId) ->
                    if (!granted) {
                        mutableConsentEnabled.value = false
                        PostHog.optOut()
                        PostHog.reset()
                        PostHog.optOut()
                        identifiedUserId = null
                        return@collect
                    }

                    if (identifiedUserId != null && identifiedUserId != userId) {
                        PostHog.reset()
                        identifiedUserId = null
                    }
                    PostHog.optIn()
                    if (userId != null && identifiedUserId != userId) {
                        PostHog.identify(userId)
                        identifiedUserId = userId
                    }
                    mutableConsentEnabled.value = true
                }
        }
    }

    suspend fun setConsent(granted: Boolean) {
        val settings = settingsRepository ?: return
        if (!granted) {
            mutableConsentEnabled.value = false
            PostHog.optOut()
            PostHog.reset()
            PostHog.optOut()
            identifiedUserId = null
        }
        settings.save(Setting(CONSENT_KEY, if (granted) GRANTED else DENIED))
    }

    fun discoveryOpened(surface: String) = capture(
        "discovery_opened",
        mapOf("surface" to surface),
    )

    fun searchSubmitted(kind: String, hasQuery: Boolean, filterKeys: List<String>) = capture(
        "search_submitted",
        mapOf(
            "kind" to kind,
            "has_query" to hasQuery,
            "filter_keys" to filterKeys,
        ),
    )

    fun shopOpened(shopId: String) = capture(
        "shop_opened",
        mapOf("shop_id" to shopId, "source" to "unspecified"),
    )

    fun coffeeOpened(coffeeSlug: String) = capture(
        "coffee_opened",
        mapOf("coffee_slug" to coffeeSlug, "source" to "unspecified"),
    )

    private fun capture(event: String, properties: Map<String, Any>) {
        if (!started || !mutableConsentEnabled.value) return
        PostHog.capture(event, properties + ("platform" to AppConfig.updatePlatform))
    }
}
