package com.coffeepeek.feature.settings.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.SettingsAccountPort
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.api.UpdateCheckPort
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.impl.ui.SettingsViewModel
import com.coffeepeek.feature.settings.impl.ui.model.SettingsAction
import com.coffeepeek.feature.settings.impl.ui.model.SettingsEvent
import com.coffeepeek.feature.settings.impl.ui.model.SettingsState
import kotlinx.coroutines.flow.map

class NativeSettingsTab(
    account: SettingsAccountPort,
    theme: ThemePort,
    cities: CityPort,
    updates: UpdateCheckPort,
    language: LanguagePort,
) {
    private val model = SettingsViewModel(account, theme, cities, updates, language)
    private val store = ViewModelStore().apply { put("settings-tab", model) }

    val state = model.state.map(SettingsState::nativeSnapshot)
    val updateMessages = model.events.map(SettingsEvent::nativeEvent)
    fun current() = model.state.value.nativeSnapshot()
    fun screenShown() = model.onScreenShown()
    fun checkForUpdates() = model.onAction(SettingsAction.CheckForUpdates)
    fun close() = store.clear()
}

data class NativeSettingsTabState(
    val showGuestCard: Boolean,
    val showAddRows: Boolean,
    val city: String?,
    val theme: ThemeMode,
    val languageTag: String,
    val checkingUpdates: Boolean,
    val updateAvailable: Boolean,
)

enum class NativeSettingsUpdateMessage { UpToDate, Failed }

private fun SettingsState.nativeSnapshot() = NativeSettingsTabState(
    showGuestCard, showAddRows, cityName, theme, language.languageTag, updateBusy, update.updateAvailable,
)

private fun SettingsEvent.nativeEvent() = when (this) {
    SettingsEvent.UpToDate -> NativeSettingsUpdateMessage.UpToDate
    SettingsEvent.UpdateCheckFailed -> NativeSettingsUpdateMessage.Failed
}
