package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.screen.shop.CheckInShopPickerScreen

/** App navigation selects the picker implementation; the feature emits only a selected public slug. */
interface CheckInShopPickerRenderer {
    @Composable
    fun Content()
}

internal object LegacyCheckInShopPickerRenderer : CheckInShopPickerRenderer {
    @Composable override fun Content() = CheckInShopPickerScreen()
}
