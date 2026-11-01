package com.coffeepeek.admin.settings

import androidx.compose.runtime.Composable
import platform.Foundation.NSUserDefaults

internal actual fun applyPlatformAppLanguage(languageTag: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(languageTag, "ru").distinct(), forKey = "AppleLanguages")
}

@Composable
internal actual fun PlatformAppLanguageContext(languageTag: String, content: @Composable () -> Unit) = content()
