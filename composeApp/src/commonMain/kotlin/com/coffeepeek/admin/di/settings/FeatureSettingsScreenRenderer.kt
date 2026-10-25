package com.coffeepeek.admin.di.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.GuestAuthCard
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.settings.api.CityEntry
import com.coffeepeek.feature.settings.api.SettingsAppInfo
import com.coffeepeek.feature.settings.api.SettingsEntry
import com.coffeepeek.feature.settings.api.SettingsSlots
import com.coffeepeek.feature.settings.api.ThemeEntry
import com.coffeepeek.feature.settings.api.UsagePolicyEntry
import com.coffeepeek.feature.settings.impl.createCityEntry
import com.coffeepeek.feature.settings.impl.createSettingsEntry
import com.coffeepeek.feature.settings.impl.createThemeEntry
import com.coffeepeek.feature.settings.impl.createUsagePolicyEntry

/** The Settings tab and sub-screens on the migrated entries; Android and iOS differ only in how a short message is shown. Android routes the theme, city and usage-policy screens to the migrated entries; navigation and the ports' targets stay app-owned. */
internal class FeatureSettingsScreenRenderer(
    private val tab: SettingsEntry,
    private val theme: ThemeEntry,
    private val city: CityEntry,
    private val policy: UsagePolicyEntry,
) : SettingsScreenRenderer {
    @Composable
    override fun Tab() = tab.Content(
        onAddShop = { Navigator.navigate(Navigator.Screen.AddShop) },
        onAddRoaster = { Navigator.navigate(Navigator.Screen.AddRoaster) },
        onCity = { Navigator.navigate(Navigator.Screen.CitySettings) },
        onTheme = { Navigator.navigate(Navigator.Screen.ThemeSettings) },
        onUsagePolicy = { Navigator.navigate(Navigator.Screen.UsagePolicy) },
        onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
        onRegister = { Navigator.navigate(Navigator.Screen.Register) },
        modifier = Modifier,
        bottomClearance = LocalFloatingNavClearance.current,
    )

    @Composable
    override fun Theme() = theme.Content(onBack = Navigator::popBack, modifier = Modifier)

    @Composable
    override fun City() = city.Content(onBack = Navigator::popBack, modifier = Modifier)

    @Composable
    override fun UsagePolicy() = policy.Content(onBack = Navigator::popBack, modifier = Modifier)
}

internal fun featureSettingsScreenRenderer(
    shops: ShopRepository, cityPreference: CityPreference, sessions: SessionReader, updates: AppUpdateController,
    /** A short message that does not take focus: a toast on Android. */
    notify: (String) -> Unit,
): FeatureSettingsScreenRenderer {
    val theme = AppThemePort()
    val cities = AppCityPort(shops, cityPreference)
    val external = AppSettingsExternalActions()
    return FeatureSettingsScreenRenderer(
        tab = createSettingsEntry(
            account = AppSettingsAccountPort(sessions), theme = theme, cities = cities, updates = AppUpdateCheckPort(updates),
            external = external, appInfo = SettingsAppInfo(AppConfig.versionName, updates.checksEnabled),
            slots = SettingsSlots(
                guestCard = { onLogin, onRegister, modifier -> GuestAuthCard(onLogin, onRegister, modifier) },
                notify = notify,
            ),
        ),
        theme = createThemeEntry(theme),
        city = createCityEntry(cities),
        policy = createUsagePolicyEntry(AppUsageConsentPort(), external),
    )
}
