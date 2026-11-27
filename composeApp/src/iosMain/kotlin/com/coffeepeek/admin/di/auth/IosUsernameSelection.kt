package com.coffeepeek.admin.di.auth

import com.coffeepeek.feature.auth.api.*
import com.coffeepeek.feature.auth.domain.isValidSelectedUsername
import com.coffeepeek.feature.settings.api.LanguagePort
import platform.UIKit.UIViewController

object IosUsernameSelectionControllers {
    private lateinit var factory: (IosUsernameSelection) -> UIViewController
    fun configure(factory: (IosUsernameSelection) -> UIViewController) { this.factory = factory }
    internal fun controller(source: IosUsernameSelection) = factory(source)
}

/** Native adapter only; the feature owns state, requests and account lifetime. */
class IosUsernameSelection internal constructor(private val gate: UsernameSelectionGate, language: LanguagePort) :
    IosAuthScreen(language, {}, {}) {
    fun currentState() = gate.state.value.snapshot()
    fun observeState(receive: (IosUsernameSelectionState) -> Unit) = observe(gate.state, { receive(it.snapshot()) })
    fun edit(value: String) = gate.onAction(UsernameSelectionAction.Edit(value))
    fun submit() = gate.onAction(UsernameSelectionAction.Submit)
    fun retry() = gate.onAction(UsernameSelectionAction.Retry)
    fun signOut() = gate.onAction(UsernameSelectionAction.SignOut)
}

data class IosUsernameSelectionState(val username: String, val editing: Boolean, val busy: Boolean,
    val failed: Boolean, val canSubmit: Boolean, val errorKey: String?)

private fun UsernameSelectionState.snapshot() = IosUsernameSelectionState(username,
    phase == UsernameSelectionPhase.Required || phase == UsernameSelectionPhase.Submitting,
    phase == UsernameSelectionPhase.Submitting || phase == UsernameSelectionPhase.Checking,
    phase == UsernameSelectionPhase.Failed, isValidSelectedUsername(username), error?.let { when (it) {
        UsernameSelectionError.Invalid -> "auth_username_invalid"
        UsernameSelectionError.Taken -> "auth_username_taken"
        UsernameSelectionError.Conflict -> "auth_username_conflict"
        UsernameSelectionError.Unavailable -> "auth_username_unavailable"
    } })
