package com.coffeepeek.admin

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.analytics.ProductAnalytics
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.domain.repository.SessionRepository
import com.posthog.kmp.PostHogContext
import org.koin.core.context.GlobalContext
import platform.UIKit.UIViewController

private var koinInitialized = false

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        ProductAnalytics.start(
            PostHogContext(),
            Locator.database.settingRepository,
            GlobalContext.get().get<SessionRepository>(),
        )
        koinInitialized = true
    }
    return ComposeUIViewController { App() }
}
