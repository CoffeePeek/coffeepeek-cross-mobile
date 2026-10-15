package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.BuildConfig
import com.posthog.kmp.PostHogContext
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import java.io.File

internal actual fun postHogContext() = PostHogContext(Locator.appContext.applicationContext as android.app.Application)
internal actual fun setupPostHog(config: PostHogConfig, context: PostHogContext) = PostHog.setup(config, context)
internal actual val analyticsDebugEnabled: Boolean get() = BuildConfig.POSTHOG_ENABLE_DEV

internal actual fun erasePostHogQueue() {
    val cache = Locator.appContext.cacheDir
    // Official Android SDK queue prefixes. Never enumerate/delete the application's cache root.
    listOf("posthog-disk-queue", "posthog-disk-replay-queue", "posthog-disk-logs-queue").forEach { prefix ->
        val parent = File(cache, prefix).canonicalFile
        val target = File(parent, POSTHOG_PROJECT_TOKEN).canonicalFile
        check(target.parentFile == parent && target.name == POSTHOG_PROJECT_TOKEN)
        check(!target.exists() || target.deleteRecursively())
    }
}
