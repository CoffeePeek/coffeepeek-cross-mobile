package com.coffeepeek.feature.settings.impl.ui.model

import com.coffeepeek.feature.settings.domain.SettingsCity

internal enum class CityLoad { Loading, Loaded, Failed }

internal data class CityState(
    val load: CityLoad = CityLoad.Loading,
    val cities: List<SettingsCity> = emptyList(),
    val selectedId: String? = null,
)

internal sealed interface CityAction {
    /** Also the retry. Ignored while a load is running. */
    data object Load : CityAction
    data class Select(val cityId: String) : CityAction
}

internal sealed interface CityEvent
