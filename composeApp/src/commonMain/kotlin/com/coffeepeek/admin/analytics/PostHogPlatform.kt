package com.coffeepeek.admin.analytics

import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext

internal expect val postHogProjectToken: String
internal expect fun postHogContext(): PostHogContext
internal expect fun setupPostHog(config: PostHogConfig, context: PostHogContext)
internal expect fun erasePostHogQueue()
internal expect val analyticsDebugEnabled: Boolean
