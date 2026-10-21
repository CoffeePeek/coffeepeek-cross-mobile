package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.impl.ui.model.ThemeAction
import com.coffeepeek.feature.settings.impl.ui.model.ThemeEvent
import com.coffeepeek.feature.settings.impl.ui.model.ThemeState
import kotlinx.coroutines.launch

/** The choice applies immediately, with no confirmation; the screen follows the app's theme, including changes made elsewhere. */
internal class ThemeViewModel(private val theme: ThemePort) : MviViewModel<ThemeState, ThemeAction, ThemeEvent>(ThemeState(theme.current())) {
    init {
        viewModelScope.launch { theme.observe().collect { mode -> updateState { copy(selected = mode) } } }
    }

    override suspend fun handleActionInternal(action: ThemeAction) {
        when (action) {
            is ThemeAction.Select -> theme.set(action.mode)
        }
    }
}
