package com.coffeepeek.admin

import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.theme.platformSystemDarkTheme
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.settings.AppLanguageEnvironment
import com.coffeepeek.admin.settings.AppLanguagePreference
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import androidx.compose.ui.Alignment
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import com.coffeepeek.admin.utils.OrientationObserver
import io.kamel.core.config.KamelConfig
import io.kamel.image.config.LocalKamelConfig
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun App(onReady: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        onReady()
    }

    AppContent {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).dismissKeyboardOnBackgroundGestures()) {
            OrientationObserver.StartObserver()
            Navigator()
            com.coffeepeek.admin.feature.authorblocking.ui.AuthorBlockingHost()
            koinInject<com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer>().Prompt()
            com.coffeepeek.admin.analytics.AnalyticsPrivacy()
        }
    }
}

/** Shared presentation environment, also used by native iOS tab controllers. */
@Composable
internal fun AppContent(content: @Composable () -> Unit) {
    AppForegroundNotifications()
    val kamelConfig = koinInject<KamelConfig>()
    val language by koinInject<AppLanguagePreference>().selection.collectAsState()
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = platformSystemDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    PlatformSystemBars(darkTheme)

    CoffeePeekTheme(darkTheme = darkTheme) {
        val selected = language
        if (selected == null) AppLanguageLoadingContent()
        else AppLanguageEnvironment(selected.languageTag) {
            CompositionLocalProvider(
                LocalKamelConfig provides kamelConfig,
                com.coffeepeek.feature.ownership.api.LocalBusinessOwnership provides koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>(),
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                content = content,
            )
        }
    }
}

@Composable
private fun AppForegroundNotifications() {
    val notifications = koinInject<com.coffeepeek.feature.notifications.api.NotificationFeature>()
    androidx.lifecycle.compose.LifecycleResumeEffect(notifications) {
        notifications.foreground()
        val heartbeat = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            while (true) {
                kotlinx.coroutines.delay(15 * 60 * 1_000L)
                notifications.foreground()
            }
        }
        onPauseOrDispose { heartbeat.cancel() }
    }
}

@Composable
private fun AppLanguageLoadingContent() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        CoffeePeekLoader()
    }
}

@Preview @Composable private fun AppLanguageLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) { AppLanguageLoadingContent() }
@Preview @Composable private fun AppLanguageLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) { AppLanguageLoadingContent() }
