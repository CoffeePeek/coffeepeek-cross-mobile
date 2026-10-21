package com.coffeepeek.feature.community.api

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Where a card or the list is measured for exposure analytics. */
enum class ExposureKind { Feed, ShopPost, HomePost }

/**
 * What the screen borrows from the application: image loading with authentication, the relative date, the brew method names, photo prefetch,
 * the full-screen photo viewer and the exposure/viewed analytics. Defaults keep previews and tests free of app services.
 */
class CommunityHost(
    val image: @Composable (url: String, highQualityUrl: String?, modifier: Modifier, contentScale: ContentScale) -> Unit = { _, _, _, _ -> },
    val displayDate: @Composable (iso: String) -> String = { it.substringBefore('T') },
    val brewMethodLabel: (code: String) -> String? = { it },
    val prefetchPhotos: @Composable (state: LazyListState, posts: List<Pair<String, List<String>>>) -> Unit = { _, _ -> },
    val photoViewer: @Composable (thumbnails: List<String>, originals: List<String>, index: Int, onDismiss: () -> Unit) -> Unit = { _, _, _, _ -> },
    val exposure: (Modifier, ExposureKind) -> Modifier = { modifier, _ -> modifier },
    val feedViewed: @Composable (firstPageCount: Int?, isPublic: Boolean) -> Unit = { _, _ -> },
)

/** «Лента». The caller owns navigation, the check-in creation flow (the [floatingAction] slot) and the ViewModel store. */
interface CommunityEntry {
    @Composable
    fun Content(
        onSignIn: () -> Unit,
        onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
        onOpenShop: (slug: String) -> Unit,
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
        floatingAction: @Composable BoxScope.() -> Unit = {},
    )
}
