package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable

/** A post opened from a shared link. Platform composition supplies the migrated entry. */
interface CheckInDetailScreenRenderer {
    @Composable
    fun Content(id: String)
}
