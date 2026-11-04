package com.coffeepeek.feature.shelf.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Profile-only shelf surfaces; creating this entry never initializes bag scanning. */
interface ShelfProfileEntry {
    @Composable
    fun Visibility(onSignIn: () -> Unit, modifier: Modifier)

    /** Published active bags only. [onOpenCoffee] receives a catalogue slug, never a private bag id. */
    @Composable
    fun Preview(profileSlug: String, onOpenCoffee: (String) -> Unit, modifier: Modifier)
}
