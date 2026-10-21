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
