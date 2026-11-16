package com.coffeepeek.feature.settings.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.settings.impl.ui.LanguageViewModel
import com.coffeepeek.feature.settings.impl.ui.model.LanguageAction
import com.coffeepeek.feature.settings.impl.ui.model.LanguageState
import kotlinx.coroutines.flow.map

/** Applies a language only after the shared app preference persists it. */
class NativeLanguageSettings(port: LanguagePort) {
    private val model = LanguageViewModel(port)
    private val store = ViewModelStore().apply { put("settings-language", model) }

    val state = model.state.map(LanguageState::nativeSnapshot)
    fun current() = model.state.value.nativeSnapshot()
    fun select(tag: String) {
        AppLanguage.entries.firstOrNull { it.languageTag == tag }?.let { model.onAction(LanguageAction.Select(it)) }
    }
    fun retry() = model.onAction(LanguageAction.RetrySave)
    fun close() = store.clear()
}

data class NativeLanguageSettingsState(val selectedTag: String, val saving: Boolean, val saveFailed: Boolean)

private fun LanguageState.nativeSnapshot() = NativeLanguageSettingsState(selected.languageTag, isSaving, hasSaveError)
