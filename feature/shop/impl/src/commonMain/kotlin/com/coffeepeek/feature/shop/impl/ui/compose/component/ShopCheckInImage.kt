package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * App-owned loader retains protected-photo permissions and session-bound byte-cache policy.
 * [url] is the small variant shown first; [highQualityUrl] is the original the loader may switch to, and the one it falls back to when the small variant fails.
 */
internal typealias ShopCheckInImage = @Composable (url: String, highQualityUrl: String?, modifier: Modifier, scale: ContentScale) -> Unit
internal val LocalShopCheckInImage = staticCompositionLocalOf<ShopCheckInImage?> { null }

/**
 * Optional app-owned prefetch for long photo lists: (list state, item key to display URLs in list order).
 * The keys must match the lazy list item keys.
 */
internal val LocalShopPhotoPrefetch = staticCompositionLocalOf<
    (@Composable (androidx.compose.foundation.lazy.LazyListState, List<Pair<String, List<String>>>) -> Unit)?> { null }
