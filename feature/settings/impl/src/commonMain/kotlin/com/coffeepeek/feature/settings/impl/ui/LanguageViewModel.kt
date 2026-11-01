package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.settings.impl.ui.model.LanguageAction
import com.coffeepeek.feature.settings.impl.ui.model.LanguageState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** Only the app's successfully persisted, observed choice becomes selected. */
internal class LanguageViewModel(private val language: LanguagePort) :
    MviViewModel<LanguageState, LanguageAction, Nothing>(LanguageState(language.current())) {
    init {
        viewModelScope.launch {
            language.observe().collect { selected ->
                updateState { copy(selected = selected, failedLanguage = failedLanguage?.takeUnless { it == selected }) }
            }
        }
    }

    override suspend fun handleActionInternal(action: LanguageAction) {
        when (action) {
            is LanguageAction.Select -> saveLanguage(action.language)
            LanguageAction.RetrySave -> currentState.failedLanguage?.let { saveLanguage(it) }
        }
    }

    private suspend fun saveLanguage(selected: AppLanguage) {
        if (currentState.isSaving) return
        if (selected == language.current()) {
            updateState { copy(failedLanguage = null) }
            return
        }
        updateState { copy(isSaving = true, failedLanguage = null) }
        try {
            if (persistLanguage(selected).isFailure) updateState { copy(failedLanguage = selected) }
        } finally {
            updateState { copy(isSaving = false) }
        }
    }

    private suspend fun persistLanguage(selected: AppLanguage): Result<Unit> {
        val result = try {
            language.set(selected)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result
    }
}
