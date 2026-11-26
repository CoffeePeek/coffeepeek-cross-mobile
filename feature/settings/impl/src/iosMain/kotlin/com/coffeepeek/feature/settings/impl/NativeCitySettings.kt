package com.coffeepeek.feature.settings.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.impl.ui.CityViewModel
import com.coffeepeek.feature.settings.impl.ui.model.CityAction
import com.coffeepeek.feature.settings.impl.ui.model.CityLoad
import com.coffeepeek.feature.settings.impl.ui.model.CityState
import kotlinx.coroutines.flow.map

/** Native rendering contract over the same catalog and selection actions as the Compose entry. */
class NativeCitySettings(port: CityPort) {
    private val model = CityViewModel(port)
    private val store = ViewModelStore().apply { put("settings-city", model) }

    val state = model.state.map(CityState::nativeSnapshot)
    fun current() = model.state.value.nativeSnapshot()
    fun retry() = model.onAction(CityAction.Load)
    fun select(id: String) = model.onAction(CityAction.Select(id))
    fun close() = store.clear()
}

data class NativeCitySettingsState(
    val cities: List<SettingsCity>,
    val selectedId: String?,
    val loading: Boolean,
    val failed: Boolean,
)

private fun CityState.nativeSnapshot() = NativeCitySettingsState(cities, selectedId, load == CityLoad.Loading, load == CityLoad.Failed)
