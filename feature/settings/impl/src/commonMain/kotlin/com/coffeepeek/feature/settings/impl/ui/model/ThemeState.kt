package com.coffeepeek.feature.settings.impl.ui.model

import com.coffeepeek.feature.settings.domain.ThemeMode

internal data class ThemeState(val selected: ThemeMode)

internal sealed interface ThemeAction {
    data class Select(val mode: ThemeMode) : ThemeAction
}

internal sealed interface ThemeEvent
