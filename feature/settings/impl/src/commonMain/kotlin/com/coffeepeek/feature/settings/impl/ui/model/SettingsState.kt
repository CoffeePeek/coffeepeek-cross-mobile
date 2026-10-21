package com.coffeepeek.feature.settings.impl.ui.model

import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.domain.UpdateCheckState

internal data class SettingsState(
    val signedIn: Boolean,
    val theme: ThemeMode,
    /** The selected city's name once the catalog is loaded; the row shows «Выберите» while it is `null`. */
    val cityName: String? = null,
    val update: UpdateCheckState = UpdateCheckState(),
    /** A check the user started is in flight (kept visible for a minimum time so the loader is not a flash). */
    val manualUpdateCheck: Boolean = false,
) {
    /** The update row is locked and shows a loader. */
    val updateBusy: Boolean get() = update.checking || manualUpdateCheck

    /** The sign-in prompt is for guests only. */
    val showGuestCard: Boolean get() = !signedIn

    /** Blocked authors are managed by a signed-in user only. */
    val showBlockedAuthors: Boolean get() = signedIn

    /** Add shop / add roaster are shown to everyone (production behaviour, owner decision 2). */
    val showAddRows: Boolean get() = true
}

internal sealed interface SettingsAction {
    /** A manual check; ignored while one is already in flight. */
    data object CheckForUpdates : SettingsAction
}

internal sealed interface SettingsEvent {
    /** The manual check found nothing newer. A newer version is offered by the app's update prompt instead. */
    data object UpToDate : SettingsEvent
    data object UpdateCheckFailed : SettingsEvent
}
