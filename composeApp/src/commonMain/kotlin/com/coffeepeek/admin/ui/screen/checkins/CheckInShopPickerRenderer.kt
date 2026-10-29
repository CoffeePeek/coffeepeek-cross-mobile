package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable

/** App navigation selects the picker implementation; the feature emits only a selected public slug. */
interface CheckInShopPickerRenderer {
    @Composable
    fun Content()
}
