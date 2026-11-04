package com.coffeepeek.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.settings.api.CityEntry
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.SettingsAccountPort
import com.coffeepeek.feature.settings.api.SettingsAppInfo
import com.coffeepeek.feature.settings.api.SettingsEntry
import com.coffeepeek.feature.settings.api.SettingsExternalActions
import com.coffeepeek.feature.settings.api.SettingsSlots
import com.coffeepeek.feature.settings.api.UpdateCheckPort
import com.coffeepeek.feature.settings.api.ThemeEntry
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.api.UsageConsentPort
import com.coffeepeek.feature.settings.api.UsagePolicyEntry
import com.coffeepeek.feature.settings.impl.ui.CityViewModel
import com.coffeepeek.feature.settings.impl.ui.SettingsViewModel
import com.coffeepeek.feature.settings.impl.ui.ThemeViewModel
import com.coffeepeek.feature.settings.impl.ui.UsagePolicyViewModel
import com.coffeepeek.feature.settings.impl.ui.compose.CityScreen
import com.coffeepeek.feature.settings.impl.ui.compose.SettingsNavigation
import com.coffeepeek.feature.settings.impl.ui.compose.SettingsScreen
import com.coffeepeek.feature.settings.impl.ui.compose.ThemeScreen
import com.coffeepeek.feature.settings.impl.ui.compose.UsagePolicyScreen

/**
 * The leaf screens keep no draft: their ViewModels live in the host's ViewModel store (the destination's, so they follow its lifetime and
 * survive rotation). The caller owns navigation; the app supplies the ports.
 */
fun createThemeEntry(theme: ThemePort): ThemeEntry = object : ThemeEntry {
    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        ThemeScreen(viewModel(key = "settings-theme") { ThemeViewModel(theme) }, onBack, modifier)
    }
}

fun createCityEntry(cities: CityPort): CityEntry = object : CityEntry {
    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        CityScreen(viewModel(key = "settings-city") { CityViewModel(cities) }, onBack, modifier)
    }
}

fun createUsagePolicyEntry(consent: UsageConsentPort, external: SettingsExternalActions): UsagePolicyEntry = object : UsagePolicyEntry {
    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        UsagePolicyScreen(
            viewModel(key = "settings-usage-policy") { UsagePolicyViewModel(consent) },
            external::openTerms, external::openPrivacy, onBack, modifier,
        )
    }
}

/** The Settings tab. The ViewModel lives in the host's store (the tab destination's). */
fun createSettingsEntry(
    account: SettingsAccountPort,
    theme: ThemePort,
    language: LanguagePort,
    cities: CityPort,
    updates: UpdateCheckPort,
    external: SettingsExternalActions,
    appInfo: SettingsAppInfo,
    slots: SettingsSlots,
): SettingsEntry = object : SettingsEntry {
    @Composable
    override fun Content(
        onAddShop: () -> Unit, onAddRoaster: () -> Unit, onCity: () -> Unit, onTheme: () -> Unit, onLanguage: () -> Unit,
        onUsagePolicy: () -> Unit, onLogin: () -> Unit, onRegister: () -> Unit, modifier: Modifier, bottomClearance: Dp,
    ) {
        SettingsScreen(
            viewModel(key = "settings-tab") { SettingsViewModel(account, theme, cities, updates, language) },
            SettingsNavigation(onAddShop, onAddRoaster, onCity, onTheme, onLanguage, onUsagePolicy, onLogin, onRegister),
            external, slots, appInfo, bottomClearance, modifier,
        )
    }
}
