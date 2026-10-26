package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInListScreenRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.createShopCheckInListEntry

internal class FeatureShopCheckInListScreenRenderer(
    private val sessions: SessionReader,
    private val accounts: ProfileAccountPort,
    private val details: ShopDetailsRepository,
    private val votes: ShopCheckInVoteRepository,
    private val visibility: com.coffeepeek.feature.authorblocking.api.AuthorVisibility,
    private val blockActions: com.coffeepeek.feature.authorblocking.api.AuthorBlockActions,
) : ShopCheckInListScreenRenderer {
    @Composable
    override fun Content(shopId: String) {
        ShopBrowsingHost("android-shop-list:$shopId", sessions, visibility) { session ->
            val entry = remember(session) {
                createShopCheckInListEntry(session.lease.details(details), session.lease.votes(votes),
                    currentViewer = { session.lease.viewer(accounts) },
                    authorImage = { url, modifier -> com.coffeepeek.admin.utils.CpImage(data = url, modifier = modifier) },
                    checkInImage = { url, highQualityUrl, modifier, scale ->
                        com.coffeepeek.admin.utils.CpImage(data = url, highQualityData = highQualityUrl, modifier = modifier, contentScale = scale)
                    },
                    photoPrefetch = { listState, photos -> com.coffeepeek.admin.utils.PrefetchListPhotos(listState, photos) })
            }
            var preview by remember { mutableStateOf<com.coffeepeek.feature.shop.api.ShopDetailEffect.OpenPhoto?>(null) }
            val formatCheckInDate = rememberCheckInDateLabel()
            entry.Content(shopId = shopId, sessionGeneration = session.generation,
                formatCheckInDate = formatCheckInDate, refreshGeneration = 0,
                onBack = Navigator::popBack,
                onSignIn = { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.Auth) },
                onRegister = { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.Register) },
                onReportCheckIn = { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.ReportCheckIn(it)) },
                onOpenAuthor = { slug, nickname, avatar ->
                    if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.UserProfile(slug, nickname, avatar))
                },
                onBlockAuthor = { session.lease.requestAuthorBlock(it, blockActions) },
                onOpenPhoto = { urls, originals, index -> if (session.lease.isCurrent())
                    preview = com.coffeepeek.feature.shop.api.ShopDetailEffect.OpenPhoto(urls, index, originals) })
            preview?.let { photo ->
                FullScreenImageDialog(images = com.coffeepeek.admin.ui.component.PhotoViewerImages(photo.imageUrls, photo.originalUrls),
                    initialIndex = photo.initialIndex, onDismiss = { preview = null })
            }
        }
    }
}
