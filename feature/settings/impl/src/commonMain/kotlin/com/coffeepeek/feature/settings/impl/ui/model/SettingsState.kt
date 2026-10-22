package com.coffeepeek.feature.settings.impl.ui.model

import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.domain.UpdateCheckState

internal data class SettingsState(
    val signedIn: Boolean,
    val theme: ThemeMode,
    /** The selected city's name once the catalog is loaded; the row shows «Выберите» while it is `null`. */
    val cityName: String? = null,
    val update: UpdateCheckState = UpdateCheckState(),
) {
    /** The sign-in prompt is for guests only. */
    val showGuestCard: Boolean get() = !signedIn

    /** Add shop / add roaster are shown to everyone (production behaviour, owner decision 2). */
    val showAddRows: Boolean get() = true
}

internal sealed interface SettingsAction {
    /** A manual check; the port ignores it while one is running. */
    data object CheckForUpdates : SettingsAction
}

internal sealed interface SettingsEvent
