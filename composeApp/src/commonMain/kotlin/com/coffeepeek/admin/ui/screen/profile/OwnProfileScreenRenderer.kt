package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable

/** Platform selection only. Shared/iOS keep the legacy Profile tab; Android composes the migrated entry. */
interface OwnProfileScreenRenderer {
    @Composable
    fun Content()
}

internal object LegacyOwnProfileScreenRenderer : OwnProfileScreenRenderer {
    @Composable
    override fun Content() = ProfileScreen()
}
