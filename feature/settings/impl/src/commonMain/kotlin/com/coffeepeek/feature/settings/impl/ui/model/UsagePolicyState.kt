package com.coffeepeek.feature.settings.impl.ui.model

import com.coffeepeek.feature.settings.domain.UsageConsentState

internal data class UsagePolicyState(val consent: UsageConsentState = UsageConsentState.Unavailable)

internal sealed interface UsagePolicyAction {
    data class SetConsent(val accepted: Boolean) : UsagePolicyAction
}

internal sealed interface UsagePolicyEvent
