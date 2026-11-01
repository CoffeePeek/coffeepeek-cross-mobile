package com.coffeepeek.admin.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag

internal expect fun applyPlatformAppLanguage(languageTag: String)

@Composable
internal expect fun PlatformAppLanguageContext(languageTag: String, content: @Composable () -> Unit)

/** Refresh resource consumers without recreating navigation or its screen state. */
@Composable
internal fun AppLanguageEnvironment(languageTag: String, content: @Composable () -> Unit) {
    PlatformAppLanguageContext(languageTag) {
        CompositionLocalProvider(LocalCoffeePeekLanguageTag provides languageTag, content = content)
    }
}
