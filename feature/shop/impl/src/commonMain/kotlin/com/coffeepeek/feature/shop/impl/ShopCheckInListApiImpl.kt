package com.coffeepeek.feature.shop.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shop.api.ShopCheckInListEntry
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInListViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInListScreen
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEvent

/** Repositories must be session-aware; app composition supplies auth and its monotonic generation. */
fun createShopCheckInListEntry(
    detailsRepository: ShopDetailsRepository,
    voteRepository: ShopCheckInVoteRepository,
    currentViewer: suspend () -> Result<ShopViewer>,
): ShopCheckInListEntry = DefaultShopCheckInListEntry(detailsRepository, voteRepository, currentViewer)

private class DefaultShopCheckInListEntry(
    private val detailsRepository: ShopDetailsRepository,
    private val voteRepository: ShopCheckInVoteRepository,
    private val currentViewer: suspend () -> Result<ShopViewer>,
) : ShopCheckInListEntry {
    @Composable
    override fun Content(
        shopId: String,
        sessionGeneration: Int,
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onRegister: () -> Unit,
        onReportCheckIn: (String) -> Unit,
        onOpenPhoto: (List<String>, Int) -> Unit,
        onOpenAuthor: (String, String, String?) -> Unit,
        refreshGeneration: Long,
    ) {
        val viewModel = viewModel(key = "shop-check-in-list:$shopId") {
            ShopCheckInListViewModel(shopId, detailsRepository, voteRepository, currentViewer,
                sessionGeneration, refreshGeneration)
        }
        ShopCheckInListScreen(viewModel, sessionGeneration, formatCheckInDate, refreshGeneration) { event ->
            when (event) {
                ShopCheckInListEvent.Back -> onBack()
                ShopCheckInListEvent.SignIn -> onSignIn()
                ShopCheckInListEvent.Register -> onRegister()
                is ShopCheckInListEvent.Report -> onReportCheckIn(event.checkInId)
                is ShopCheckInListEvent.OpenPhoto -> onOpenPhoto(event.urls, event.index)
                is ShopCheckInListEvent.OpenAuthor -> onOpenAuthor(event.slug, event.nickname, event.avatarUrl)
            }
        }
    }
}
