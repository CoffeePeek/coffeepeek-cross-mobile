package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable

/** Both platforms bind feature entries; the legacy Profile tab remains a fallback. */
interface OwnProfileScreenRenderer {
    @Composable
    fun Content()
}

internal object LegacyOwnProfileScreenRenderer : OwnProfileScreenRenderer {
    @Composable
    override fun Content() = ProfileScreen()
}
