package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.impl.ui.model.CityAction
import com.coffeepeek.feature.settings.impl.ui.model.CityEvent
import com.coffeepeek.feature.settings.impl.ui.model.CityLoad
import com.coffeepeek.feature.settings.impl.ui.model.CityState
import kotlinx.coroutines.launch

/**
 * The default city. Unlike production (an empty card when the catalog is missing) a failed load shows an error with a retry. Only ids
 * from the loaded list can be selected; a repeated choice is ignored by the port.
 */
internal class CityViewModel(private val port: CityPort) : MviViewModel<CityState, CityAction, CityEvent>(CityState()) {
    init {
        viewModelScope.launch { port.observeSelectedCityId().collect { id -> updateState { copy(selectedId = id) } } }
        onAction(CityAction.Load)
    }

    override suspend fun handleActionInternal(action: CityAction) {
        when (action) {
            CityAction.Load -> load()
            is CityAction.Select -> if (currentState.cities.any { it.id == action.cityId }) port.select(action.cityId)
        }
    }

    private var loading = false

    private suspend fun load() {
        if (loading || (currentState.load == CityLoad.Loaded && currentState.cities.isNotEmpty())) return
        loading = true
        updateState { copy(load = CityLoad.Loading) }
        try {
            port.loadCities()
                .onSuccess { list -> updateState { copy(load = CityLoad.Loaded, cities = list) } }
                .onFailure { updateState { copy(load = CityLoad.Failed) } }
        } finally {
            loading = false
        }
    }
}
