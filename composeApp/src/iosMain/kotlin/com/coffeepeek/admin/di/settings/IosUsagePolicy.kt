package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.SettingsExternalActions
import com.coffeepeek.feature.settings.domain.UsageConsentState
import com.coffeepeek.feature.settings.impl.NativeUsagePolicy
import kotlinx.coroutines.flow.map

class IosUsagePolicy internal constructor(
    private val presentation: NativeUsagePolicy,
    language: LanguagePort,
    onBack: () -> Unit,
    private val external: SettingsExternalActions,
) : IosSettingsScreen(language, onBack, presentation::close) {
    fun currentState() = presentation.current().toIos()
    fun observeState(onState: (IosUsagePolicyState) -> Unit) = observe(presentation.state.map { it.toIos() }, onState)
    fun setConsent(accepted: Boolean) = presentation.setConsent(accepted)
    fun openTerms() = external.openTerms()
    fun openPrivacy() = external.openPrivacy()
}

data class IosUsagePolicyState(val available: Boolean, val accepted: Boolean, val syncFailed: Boolean)

private fun UsageConsentState.toIos() = IosUsagePolicyState(available, accepted, syncFailed)
