package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.staticCompositionLocalOf
internal data class ShopBusinessIdentity(val id: String, val slug: String)
internal val LocalShopBusinessIdentity = staticCompositionLocalOf<ShopBusinessIdentity?> { null }
