package com.coffeepeek.feature.discovery.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Destination-owned shop selection. The caller decides navigation and owns the check-in tracker. */
interface CheckInShopPickerEntry {
    @Composable
    fun Content(onBack: () -> Unit, onClose: () -> Unit, onSelectShop: (String) -> Unit,
        onSignIn: () -> Unit, modifier: Modifier)
}
