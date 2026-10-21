package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable

/** Platform selection only. Shared/iOS keep the legacy Settings tab and sub-screens; Android composes the migrated entries. */
interface SettingsScreenRenderer {
    @Composable
    fun Tab()

    @Composable
    fun Theme()

    @Composable
    fun City()

    @Composable
    fun UsagePolicy()
}

internal object LegacySettingsScreenRenderer : SettingsScreenRenderer {
    @Composable
    override fun Tab() = SettingsScreen()

    @Composable
    override fun Theme() = ThemeScreen()

    @Composable
    override fun City() = CityScreen()

    @Composable
    override fun UsagePolicy() = UsagePolicyScreen()
}
