package com.coffeepeek.feature.auth.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

enum class UsernameSelectionPhase { SignedOut, Checking, Required, Submitting, Ready, Failed }
enum class UsernameSelectionError { Invalid, Taken, Conflict, Unavailable }

data class UsernameSelectionState(
    val phase: UsernameSelectionPhase = UsernameSelectionPhase.Checking,
    val accountId: String? = null,
    val accountGeneration: Long = 0,
    val username: String = "",
    val error: UsernameSelectionError? = null,
) {
    val blocksApp get() = phase != UsernameSelectionPhase.SignedOut && phase != UsernameSelectionPhase.Ready
    fun readyFor(account: String?, generation: Long): Boolean =
        phase == UsernameSelectionPhase.Ready && accountId == account && accountGeneration == generation
}

sealed interface UsernameSelectionAction {
    data class Edit(val value: String) : UsernameSelectionAction
    data object Submit : UsernameSelectionAction
    data object Retry : UsernameSelectionAction
    data object SignOut : UsernameSelectionAction
}

/** Authenticated credentials remain valid while this separate account-setup gate is closed. */
interface UsernameSelectionGate {
    val state: StateFlow<UsernameSelectionState>
    fun onAction(action: UsernameSelectionAction)
}

interface UsernameSelectionEntry {
    @Composable
    fun Content(modifier: Modifier)
}
