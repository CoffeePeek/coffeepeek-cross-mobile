package com.coffeepeek.feature.community.api

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Where a card or the list is measured for exposure analytics. */
enum class ExposureKind { Feed, ShopPost, HomePost }

/**
 * What the screen borrows from the application: image loading with authentication, the relative date, the brew method names, photo prefetch,
 * the full-screen photo viewer, the snackbar look (it must clear the floating navigation) and the exposure/viewed analytics. Defaults keep previews and tests free of app services.
 */
class CommunityHost(
    val image: @Composable (url: String, highQualityUrl: String?, modifier: Modifier, contentScale: ContentScale) -> Unit = { _, _, _, _ -> },
    val displayDate: @Composable (iso: String) -> String = { it.substringBefore('T') },
    val brewMethodLabel: (code: String) -> String? = { it },
    val prefetchPhotos: @Composable (state: LazyListState, posts: List<Pair<String, List<String>>>) -> Unit = { _, _ -> },
    val photoViewer: @Composable (thumbnails: List<String>, originals: List<String>, index: Int, onDismiss: () -> Unit) -> Unit = { _, _, _, _ -> },
    val exposure: (Modifier, ExposureKind) -> Modifier = { modifier, _ -> modifier },
    val feedViewed: @Composable (firstPageCount: Int?, isPublic: Boolean) -> Unit = { _, _ -> },
    val snackbar: @Composable (state: SnackbarHostState, bottomClearance: Dp) -> Unit = { state, _ -> SnackbarHost(state) },
)

/** What the floating action slot needs: whether a user is signed in, when the account or scope changes (close open sheets), and how to reload. */
class CommunityFloating(val isLoggedIn: Boolean?, val sessionGeneration: Long, val refresh: () -> Unit)

/**
 * «Лента». The caller owns navigation, the check-in creation flow (the [floatingAction] slot) and the ViewModel store.
 * [onEditHome] opens the Home editor for a home preparation; the list reloads when the user comes back.
 */
interface CommunityEntry {
    @Composable
    fun Content(
        onSignIn: () -> Unit,
        onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onEditHome: (id: String) -> Unit,
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
        floatingAction: @Composable BoxScope.(CommunityFloating) -> Unit = {},
    )
}
