package com.coffeepeek.admin.di.settings

import android.widget.Toast
import com.coffeepeek.admin.locator.Locator
import androidx.compose.runtime.Composable
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

/** Android routes the theme, city and usage-policy screens to the migrated entries; navigation and the ports' targets stay app-owned. */
internal class AndroidSettingsScreenRenderer(
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
        bottomClearance = LocalFloatingNavClearance.current,
    )

    @Composable
    override fun Theme() = theme.Content(onBack = Navigator::popBack)

    @Composable
    override fun City() = city.Content(onBack = Navigator::popBack)

    @Composable
    override fun UsagePolicy() = policy.Content(onBack = Navigator::popBack)
}

internal fun androidSettingsScreenRenderer(
    shops: ShopRepository, cityPreference: CityPreference, sessions: SessionReader, updates: AppUpdateController,
): AndroidSettingsScreenRenderer {
    val theme = AndroidThemePort()
    val cities = AndroidCityPort(shops, cityPreference)
    val external = AndroidSettingsExternalActions()
    return AndroidSettingsScreenRenderer(
        tab = createSettingsEntry(
            account = AndroidSettingsAccountPort(sessions), theme = theme, cities = cities, updates = AndroidUpdateCheckPort(updates),
            external = external, appInfo = SettingsAppInfo(AppConfig.versionName),
            slots = SettingsSlots(
                guestCard = { onLogin, onRegister, modifier -> GuestAuthCard(onLogin, onRegister, modifier) },
                notify = { Toast.makeText(Locator.appContext, it, Toast.LENGTH_SHORT).show() },
            ),
        ),
        theme = createThemeEntry(theme),
        city = createCityEntry(cities),
        policy = createUsagePolicyEntry(AndroidUsageConsentPort(), external),
    )
}
