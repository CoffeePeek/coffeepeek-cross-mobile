package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.settings.api.UsageConsentPort
import com.coffeepeek.feature.settings.impl.ui.model.UsagePolicyAction
import com.coffeepeek.feature.settings.impl.ui.model.UsagePolicyEvent
import com.coffeepeek.feature.settings.impl.ui.model.UsagePolicyState
import kotlinx.coroutines.launch

/** The consent switch follows the app's analytics state (it exists only for a signed-in user); the port applies the choice. */
internal class UsagePolicyViewModel(private val consent: UsageConsentPort) :
    MviViewModel<UsagePolicyState, UsagePolicyAction, UsagePolicyEvent>(UsagePolicyState()) {
    init {
        viewModelScope.launch { consent.observe().collect { state -> updateState { copy(consent = state) } } }
    }

    override suspend fun handleActionInternal(action: UsagePolicyAction) {
        when (action) {
            is UsagePolicyAction.SetConsent -> if (currentState.consent.available) consent.set(action.accepted)
        }
    }
}
