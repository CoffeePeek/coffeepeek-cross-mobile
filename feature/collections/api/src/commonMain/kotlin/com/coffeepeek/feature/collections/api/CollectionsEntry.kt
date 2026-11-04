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

    @Composable
    fun Library(onBack: () -> Unit, onSignIn: () -> Unit, onOpenEditor: (String?) -> Unit, modifier: Modifier)

    @Composable
    fun Editor(id: String?, onBack: () -> Unit, onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier)

    @Composable
    fun Picker(target: CollectionTarget, onDismiss: () -> Unit, onSignIn: () -> Unit, modifier: Modifier)

    /** Profile visibility switch and a modal single-choice picker for public collections. */
    @Composable
    fun ProfilePreference(onSignIn: () -> Unit, modifier: Modifier)

    /** Selected public collection for the owner; [fallback] is used only after a successful opt-out read. */
    @Composable
    fun OwnerProfile(onOpenCollection: (String) -> Unit, onSignIn: () -> Unit, fallback: @Composable () -> Unit, modifier: Modifier)

    /** Shows only the explicitly featured public collection, or no content. */
    @Composable
    fun Featured(profileSlug: String, onOpenCollection: (String) -> Unit, modifier: Modifier)

    @Composable
    fun SavedPublic(id: String, onBack: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier)
}
