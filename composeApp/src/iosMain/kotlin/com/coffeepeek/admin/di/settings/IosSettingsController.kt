package com.coffeepeek.admin.di.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.discovery.api.PublicDiscoveryCatalogs
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.SettingsAppInfo
import com.coffeepeek.feature.settings.impl.NativeCitySettings
import com.coffeepeek.feature.settings.impl.NativeLanguageSettings
import com.coffeepeek.feature.settings.impl.NativeSettingsTab
import com.coffeepeek.feature.settings.impl.NativeUsagePolicy
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosSettingsController = staticCompositionLocalOf<(IosSettingsScreen) -> UIViewController> {
    error("The iOS app must provide a settings controller")
}

@Composable
internal fun iosSettingsContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val factory = LocalIosSettingsController.current
    val dependencies = IosSettingsDependencies(
        koinInject(), koinInject(), koinInject(), koinInject(), koinInject(), koinInject(), koinInject(),
    )
    return { route -> dependencies.controller(route, factory) }
}

private class IosSettingsDependencies(
    catalogs: PublicDiscoveryCatalogs,
    cityPreference: CityPreference,
    private val sessions: SessionReader,
    private val updates: AppUpdateController,
    private val language: LanguagePort,
    private val notifications: NotificationFeature,
    private val ownership: BusinessOwnershipEntry,
) {
    private val cities = AppCityPort(catalogs, cityPreference)
    private val external = AppSettingsExternalActions()

    fun controller(route: Navigator.Screen, factory: (IosSettingsScreen) -> UIViewController): (() -> UIViewController)? = when (route) {
        Navigator.Screen.SettingsTab -> { { factory(tab()) } }
        Navigator.Screen.CitySettings -> { { factory(IosCitySettings(NativeCitySettings(cities), language, Navigator::popBack)) } }
        Navigator.Screen.LanguageSettings -> { { factory(IosLanguageSettings(NativeLanguageSettings(language), language, Navigator::popBack)) } }
        Navigator.Screen.UsagePolicy -> { { factory(policy()) } }
        else -> null
    }

    private fun tab() = IosSettingsTab(
        NativeSettingsTab(AppSettingsAccountPort(sessions), AppThemePort(), cities, AppUpdateCheckPort(updates), language),
        language, notifications, ownership, external, SettingsAppInfo(AppConfig.versionName, updates.checksEnabled), Navigator::navigate,
    )

    private fun policy() = IosUsagePolicy(NativeUsagePolicy(AppUsageConsentPort()), language, Navigator::popBack, external)
}
