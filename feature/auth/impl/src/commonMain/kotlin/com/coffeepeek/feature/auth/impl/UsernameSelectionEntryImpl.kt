package com.coffeepeek.feature.auth.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.auth.api.UsernameSelectionEntry
import com.coffeepeek.feature.auth.api.UsernameSelectionGate
import com.coffeepeek.feature.auth.impl.ui.UsernameSelectionScreenContent

fun createUsernameSelectionEntry(gate: UsernameSelectionGate): UsernameSelectionEntry = object : UsernameSelectionEntry {
    @Composable
    override fun Content(modifier: Modifier) {
        val state by gate.state.collectAsStateWithLifecycle()
        UsernameSelectionScreenContent(state, gate::onAction, modifier)
    }
}
