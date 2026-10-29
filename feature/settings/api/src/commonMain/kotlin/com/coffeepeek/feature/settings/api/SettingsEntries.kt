package com.coffeepeek.feature.settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Тема". Caller owns navigation and the destination's ViewModel store.
 * The entries declare no default arguments: Kotlin/Native links an override of a composable with a default as a different signature.
 */
interface ThemeEntry {
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier)
}

/** "Город". Loads the catalog itself, with loading, error and retry states. */
interface CityEntry {
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier)
}

/** "Политика использования": the analytics consent switch (when available) and the terms/privacy links. */
interface UsagePolicyEntry {
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier)
}

/** App-rendered pieces of the Settings tab. */
class SettingsSlots(
    /** The sign-in prompt card shared with other screens (login and register actions are the tab's own callbacks). */
    val guestCard: @Composable (onLogin: () -> Unit, onRegister: () -> Unit, modifier: Modifier) -> Unit,
    /** A short message that does not take focus (a toast on Android); shown for the update check outcome. */
    val notify: (String) -> Unit,
    val accountSwitch: @Composable () -> Unit = {},
)

/** The Settings tab. Caller owns the tab, navigation and the floating bar clearance. */
interface SettingsEntry {
    @Composable
    fun Content(
        onAddShop: () -> Unit,
        onAddRoaster: () -> Unit,
        onCity: () -> Unit,
        onTheme: () -> Unit,
        onUsagePolicy: () -> Unit,
        onLogin: () -> Unit,
        onRegister: () -> Unit,
        modifier: Modifier,
        bottomClearance: androidx.compose.ui.unit.Dp,
    )
}
