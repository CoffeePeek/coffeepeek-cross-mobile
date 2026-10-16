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
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.createShopCheckInListEntry

internal class AndroidShopCheckInListScreenRenderer(
    private val sessions: SessionRepository,
    private val users: UserRepository,
    private val details: ShopDetailsRepository,
    private val votes: ShopCheckInVoteRepository,
) : ShopCheckInListScreenRenderer {
    @Composable
    override fun Content(shopId: String) {
        AndroidShopBrowsingHost("android-shop-list:$shopId", sessions) { session ->
            val entry = remember(session) {
                createShopCheckInListEntry(session.lease.details(details), session.lease.votes(votes),
                    currentViewer = { session.lease.viewer(users) })
            }
            var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
            val formatCheckInDate = rememberCheckInDateLabel()
            entry.Content(shopId = shopId, sessionGeneration = session.generation,
                formatCheckInDate = formatCheckInDate,
                onBack = Navigator::popBack,
                onSignIn = { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.Auth) },
                onRegister = { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.Register) },
                onReportCheckIn = { if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.ReportCheckIn(it)) },
                onOpenAuthor = { slug, nickname, avatar ->
                    if (session.lease.isCurrent()) Navigator.navigate(Navigator.Screen.UserProfile(slug, nickname, avatar))
                },
                onOpenPhoto = { urls, index -> if (session.lease.isCurrent()) preview = urls to index })
            preview?.let { (urls, index) ->
                FullScreenImageDialog(imageUrls = urls, initialIndex = index, onDismiss = { preview = null })
            }
        }
    }
}
