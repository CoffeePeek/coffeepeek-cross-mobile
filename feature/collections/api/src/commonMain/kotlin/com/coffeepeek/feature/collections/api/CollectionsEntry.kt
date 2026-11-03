package com.coffeepeek.feature.collections.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Application owns routes, sign-in and the native share sheet. No defaulted Native ABI parameters. */
interface CollectionsEntry {
    @Composable
    fun Composer(onBack: () -> Unit, onSignIn: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, onShareText: (String) -> Unit,
        onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier)

    @Composable
    fun Public(id: String, onBack: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier)
}
