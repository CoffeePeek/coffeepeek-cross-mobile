package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable

/** Application destination that loads a place and hosts its check-in editor. */
interface ShopCheckInEditorRenderer {
    @Composable
    fun Content(shopId: String)
}
