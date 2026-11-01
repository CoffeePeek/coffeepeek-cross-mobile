package com.coffeepeek.admin.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.coffeepeek.admin.locator.Locator
import java.util.Locale

private val appRegion by lazy { Locale.getDefault().country }

private fun appLocale(tag: String): Locale = Locale.Builder().setLanguage(tag).apply {
    if (appRegion.isNotEmpty()) setRegion(appRegion)
}.build()

@Suppress("DEPRECATION")
internal actual fun applyPlatformAppLanguage(languageTag: String) {
    val locale = appLocale(languageTag)
    if (Locale.getDefault() != locale) Locale.setDefault(locale)
    val resources = (Locator.platformContext as? Context)?.resources ?: return
    val configuration = languageConfiguration(resources.configuration, languageTag)
    if (resources.configuration != configuration) {
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}

@Suppress("DEPRECATION")
private fun languageConfiguration(base: Configuration, tag: String) = Configuration(base).apply {
    val locale = appLocale(tag)
    if (Build.VERSION.SDK_INT >= 24) {
        setLocales(if (tag == "ru") LocaleList(locale) else LocaleList(locale, appLocale("ru")))
    } else setLocale(locale)
}

@Composable
internal actual fun PlatformAppLanguageContext(languageTag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(configuration, languageTag) { languageConfiguration(configuration, languageTag) }
    val context = remember(base, localized) {
        ContextThemeWrapper(base, base.theme).apply { applyOverrideConfiguration(localized) }
    }
    SideEffect { applyPlatformAppLanguage(languageTag) }
    CompositionLocalProvider(LocalConfiguration provides localized, LocalContext provides context, content = content)
}
