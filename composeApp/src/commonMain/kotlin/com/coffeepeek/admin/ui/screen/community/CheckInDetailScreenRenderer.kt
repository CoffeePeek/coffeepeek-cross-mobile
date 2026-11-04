package com.coffeepeek.admin.ui.screen.community

import androidx.compose.runtime.Composable

/** A post opened from a shared link. Platform composition supplies the migrated entry. */
interface CheckInDetailScreenRenderer {
    @Composable
    fun Content(id: String)
}
