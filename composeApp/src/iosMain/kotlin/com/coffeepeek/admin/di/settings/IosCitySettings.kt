package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.impl.NativeCitySettings
import com.coffeepeek.feature.settings.impl.NativeCitySettingsState
import kotlinx.coroutines.flow.map

class IosCitySettings internal constructor(
    private val presentation: NativeCitySettings,
    language: LanguagePort,
    onBack: () -> Unit,
) : IosSettingsScreen(language, onBack, presentation::close) {
    fun currentState() = presentation.current().toIos()
    fun observeState(onState: (IosCitySettingsState) -> Unit) = observe(presentation.state.map { it.toIos() }, onState)
    fun select(id: String) = presentation.select(id)
    fun retry() = presentation.retry()
}

data class IosSettingsCity(val id: String, val name: String)

data class IosCitySettingsState(
    val cities: List<IosSettingsCity>,
    val selectedId: String?,
    val loading: Boolean,
    val failed: Boolean,
)

private fun NativeCitySettingsState.toIos() = IosCitySettingsState(
    cities.map { IosSettingsCity(it.id, it.name) }, selectedId, loading, failed,
)
