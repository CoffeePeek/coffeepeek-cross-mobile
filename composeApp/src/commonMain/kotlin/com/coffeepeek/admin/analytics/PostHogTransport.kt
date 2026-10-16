package com.coffeepeek.admin.analytics

import com.posthog.kmp.ErrorTrackingConfig
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogBeforeSend
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext

internal expect val postHogProjectToken: String
internal expect fun postHogContext(): PostHogContext
internal expect fun setupPostHog(config: PostHogConfig, context: PostHogContext)
internal expect fun erasePostHogQueue()
internal expect val analyticsDebugEnabled: Boolean

internal class PostHogTransport : AnalyticsTransport {
    private var started = false
    override fun start(allow: () -> Boolean, userId: String?) {
        // Offline events survive transient network loss while foregrounded. A new run never
        // replays old consent/account events. SDK owns batching and the only offline queue.
        erasePostHogQueue()
        setupPostHog(
            PostHogConfig(
                apiKey = postHogProjectToken,
                host = PostHogConfig.HOST_EU,
                captureApplicationLifecycleEvents = false,
                captureScreenViews = false,
                captureDeepLinks = false,
                sendFeatureFlagEvent = false,
                preloadFeatureFlags = false,
                autocapture = false,
                sessionRecording = null,
                errorTracking = ErrorTrackingConfig(autoCapture = false),
                maxQueueSize = 1000,
                maxBatchSize = 50,
                beforeSend = listOf(PostHogBeforeSend { event ->
                    if (!allow()) null else sanitizeSdkProperties(event.event, event.properties)?.let { event.copy(properties = it) }
                }),
            ), postHogContext(),
        )
        started = true
        // Never restore an account from SDK storage after the application has logged it out.
        val previous = PostHog.getDistinctId()
        if (previous != null && previous != PostHog.getAnonymousId() && previous != userId) PostHog.reset()
        PostHog.optIn()
    }
    override fun identify(userId: String) { PostHog.identify(userId) }
    override fun capture(event: String, properties: Map<String, Any>) { PostHog.capture(event, properties) }
    override fun screen(page: String, properties: Map<String, Any>) { PostHog.screen(page, properties) }
    override fun stopDelivery() { if (started) PostHog.optOut() }
    override fun erase(resetIdentity: Boolean) {
        var failure: Throwable? = null
        fun attempt(block: () -> Unit) { runCatching(block).onFailure { if (failure == null) failure = it } }
        if (started) {
            attempt { PostHog.optOut() }
            if (resetIdentity) attempt { PostHog.reset() }
            attempt { PostHog.optOut() }
            attempt { PostHog.close() }
            started = false
        }
        attempt { erasePostHogQueue() }
        failure?.let { throw IllegalStateException("Analytics cleanup failed", it) }
    }
}

/** Strip SDK-enriched text, URLs, person properties and any unexpected automatic events. */
internal fun sanitizeSdkProperties(name: String, properties: Map<String, Any?>): Map<String, Any?>? {
    val event = AnalyticsEvent.entries.firstOrNull { it.wire == name }
    if (event == null && name !in setOf("\$screen", "\$identify")) return null
    val business = event?.let { (it.required + " " + it.optional + " city_slug").split(' ').toSet() }.orEmpty()
    val allowed = business + setOf("app", "event_schema_version", "app_version", "environment", "auth_state", "page_type",
        "\$screen_name", "\$screen_title", "\$lib", "\$lib_version", "\$os", "\$os_name", "\$os_version", "\$device_type",
        "\$manufacturer", "\$model", "\$app_version", "\$app_build", "\$app_namespace", "\$session_id",
        "\$anon_distinct_id", "\$is_identified", "\$process_person_profile")
    val result = properties.filterKeys { it in allowed }
    // 8 KiB payload ceiling * 1000 SDK records bounds retained property data to ~8 MiB.
    if (result.toString().encodeToByteArray().size > 8192) return null
    return result
}
