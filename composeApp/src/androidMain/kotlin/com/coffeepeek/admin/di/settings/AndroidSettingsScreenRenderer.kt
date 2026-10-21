package com.coffeepeek.admin.di.settings

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.settings.api.CityEntry
import com.coffeepeek.feature.settings.api.ThemeEntry
import com.coffeepeek.feature.settings.api.UsagePolicyEntry
import com.coffeepeek.feature.settings.impl.createCityEntry
import com.coffeepeek.feature.settings.impl.createThemeEntry
import com.coffeepeek.feature.settings.impl.createUsagePolicyEntry

/** Android routes the theme, city and usage-policy screens to the migrated entries; navigation and the ports' targets stay app-owned. */
internal class AndroidSettingsScreenRenderer(
    private val theme: ThemeEntry,
    private val city: CityEntry,
    private val policy: UsagePolicyEntry,
) : SettingsScreenRenderer {
    @Composable
    override fun Theme() = theme.Content(onBack = Navigator::popBack)

    @Composable
    override fun City() = city.Content(onBack = Navigator::popBack)

    @Composable
    override fun UsagePolicy() = policy.Content(onBack = Navigator::popBack)
}

internal fun androidSettingsScreenRenderer(shops: ShopRepository, cityPreference: CityPreference) = AndroidSettingsScreenRenderer(
    theme = createThemeEntry(AndroidThemePort()),
    city = createCityEntry(AndroidCityPort(shops, cityPreference)),
    policy = createUsagePolicyEntry(AndroidUsageConsentPort(), AndroidSettingsExternalActions()),
)
