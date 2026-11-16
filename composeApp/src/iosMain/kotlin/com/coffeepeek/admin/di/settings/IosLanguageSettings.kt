package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.impl.NativeLanguageSettings
import com.coffeepeek.feature.settings.impl.NativeLanguageSettingsState
import kotlinx.coroutines.flow.map

class IosLanguageSettings internal constructor(
    private val presentation: NativeLanguageSettings,
    language: LanguagePort,
    onBack: () -> Unit,
) : IosSettingsScreen(language, onBack, presentation::close) {
    fun currentState() = presentation.current().toIos()
    fun observeState(onState: (IosLanguageSettingsState) -> Unit) = observe(presentation.state.map { it.toIos() }, onState)
    fun select(tag: String) = presentation.select(tag)
    fun retry() = presentation.retry()
}

data class IosLanguageSettingsState(val selectedTag: String, val saving: Boolean, val saveFailed: Boolean)

private fun NativeLanguageSettingsState.toIos() = IosLanguageSettingsState(selectedTag, saving, saveFailed)
