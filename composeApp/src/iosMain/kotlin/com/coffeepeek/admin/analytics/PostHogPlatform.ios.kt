@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.analytics

import com.posthog.kmp.PostHogContext
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogEvent
import platform.Foundation.*
import swiftPMImport.com.posthog.posthog.kmp.BoxedBeforeSendBlock
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import swiftPMImport.com.posthog.posthog.kmp.PostHogSDK
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesIdentifiedOnly

internal actual fun postHogContext() = PostHogContext()

/**
 * KMP 0.5.1 does not expose surveys/enableSwizzling; native 3.64.1 defaults surveys
 * to true. Bootstrap the SAME native singleton the official KMP methods delegate to.
 * No second setup/instance, fork, reflection or private SDK API.
 */
internal actual fun setupPostHog(config: PostHogConfig, context: PostHogContext) {
    val native = NativePostHogConfig(apiKey = config.apiKey, host = config.host).apply {
        debug = false
        surveys = false
        enableSwizzling = false
        captureApplicationLifecycleEvents = false
        captureScreenViews = false
        captureElementInteractions = false
        sendFeatureFlagEvent = false
        preloadFeatureFlags = false
        sessionReplay = false
        errorTrackingConfig.autoCapture = false
        setDefaultPersonProperties = false
        personProfiles = PostHogPersonProfilesIdentifiedOnly
        maxQueueSize = config.maxQueueSize.toLong()
        maxBatchSize = config.maxBatchSize.toLong()
        flushAt = config.flushAt.toLong()
        flushIntervalSeconds = config.flushIntervalSeconds.toDouble()
        optOut = config.optOut
        setBeforeSend(listOf(BoxedBeforeSendBlock { event ->
            if (event == null) null else {
                val original = event.properties
                var processed: PostHogEvent? = PostHogEvent(event.event, event.distinctId,
                    original.entries.mapNotNull { (key, value) -> (key as? String)?.let { it to value } }.toMap())
                for (callback in config.beforeSend) {
                    processed = processed?.let { current -> runCatching { callback.run(current) }.getOrNull() }
                }
                processed?.let { safe ->
                    // Preserve NSCFBoolean values. Re-boxing all Kotlin map values can turn
                    // native JSON booleans into numbers; remove unwanted entries natively.
                    val filtered = NSMutableDictionary()
                    filtered.addEntriesFromDictionary(original)
                    original.keys.forEach { key -> if (key !is String || key !in safe.properties) filtered.removeObjectForKey(key) }
                    @Suppress("UNCHECKED_CAST")
                    event.properties = filtered.copy() as Map<Any?, *>
                    event
                }
            }
        }))
    }
    PostHogSDK.shared.setup(native)
}
internal actual val analyticsDebugEnabled: Boolean get() =
    NSBundle.mainBundle.objectForInfoDictionaryKey("PostHogEnableDev").let { it == true || it == "YES" || it == "true" }

internal actual fun erasePostHogQueue() {
    val bundle = NSBundle.mainBundle.bundleIdentifier ?: return
    val support = NSFileManager.defaultManager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask).firstOrNull() as? NSURL ?: return
    val project = support.URLByAppendingPathComponent(bundle)?.URLByAppendingPathComponent(POSTHOG_PROJECT_TOKEN) ?: return
    // The pinned SDK uses Application Support/<bundle>/<project>/posthog.queue.
    listOf("posthog.queue", "posthog.replayQueue", "posthog.logsQueue").forEach { name ->
        val target = project.URLByAppendingPathComponent(name) ?: return@forEach
        if (NSFileManager.defaultManager.fileExistsAtPath(target.path.orEmpty())) {
            check(NSFileManager.defaultManager.removeItemAtURL(target, null))
        }
    }
}
