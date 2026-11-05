package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface OwnProfileScreenRenderer {
    @Composable
    fun Content()
}
