package com.coffeepeek.feature.settings.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.settings.api.UsageConsentPort
import com.coffeepeek.feature.settings.impl.ui.UsagePolicyViewModel
import com.coffeepeek.feature.settings.impl.ui.model.UsagePolicyAction
import kotlinx.coroutines.flow.map

/** Exposes the consent state without creating another consent writer. */
class NativeUsagePolicy(port: UsageConsentPort) {
    private val model = UsagePolicyViewModel(port)
    private val store = ViewModelStore().apply { put("settings-usage-policy", model) }

    val state = model.state.map { it.consent }
    fun current() = model.state.value.consent
    fun setConsent(accepted: Boolean) = model.onAction(UsagePolicyAction.SetConsent(accepted))
    fun close() = store.clear()
}
