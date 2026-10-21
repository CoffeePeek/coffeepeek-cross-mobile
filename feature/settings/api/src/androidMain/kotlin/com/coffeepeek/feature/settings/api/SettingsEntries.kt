package com.coffeepeek.feature.settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** "Тема". Caller owns navigation and the destination's ViewModel store. */
interface ThemeEntry {
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier = Modifier)
}

/** "Город". Loads the catalog itself, with loading, error and retry states. */
interface CityEntry {
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier = Modifier)
}

/** "Политика использования": the analytics consent switch (when available) and the terms/privacy links. */
interface UsagePolicyEntry {
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier = Modifier)
}

/** App-rendered pieces of the Settings tab. */
class SettingsSlots(
    /** The sign-in prompt card shared with other screens (login and register actions are the tab's own callbacks). */
    val guestCard: @Composable (onLogin: () -> Unit, onRegister: () -> Unit, modifier: Modifier) -> Unit,
)

/** The Settings tab. Caller owns the tab, navigation and the floating bar clearance. */
interface SettingsEntry {
    @Composable
    fun Content(
        onAddShop: () -> Unit,
        onAddRoaster: () -> Unit,
        onBlockedAuthors: () -> Unit,
        onCity: () -> Unit,
        onTheme: () -> Unit,
        onUsagePolicy: () -> Unit,
        onLogin: () -> Unit,
        onRegister: () -> Unit,
        modifier: Modifier = Modifier,
        bottomClearance: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Hairline,
    )
}
