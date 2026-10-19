package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/** App-owned loader retains protected-photo permissions and session-bound byte-cache policy. */
internal typealias ShopCheckInImage = @Composable (url: String, modifier: Modifier, scale: ContentScale) -> Unit
internal val LocalShopCheckInImage = staticCompositionLocalOf<ShopCheckInImage?> { null }
