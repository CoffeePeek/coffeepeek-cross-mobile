package com.coffeepeek.feature.settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The shared two-language selector. Caller owns navigation and the destination's ViewModel store. */
interface LanguageEntry {
    /** Explicit arguments keep the abstract Compose signature identical across Kotlin/Native module boundaries. */
    @Composable
    fun Content(onBack: () -> Unit, modifier: Modifier)
}
