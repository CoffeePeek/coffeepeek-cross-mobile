package com.coffeepeek.feature.shop.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.api.ShopDetailEffect
import com.coffeepeek.feature.shop.api.ShopDetailEntry
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.ShopDetailViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.ShopDetailScreen
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent

/** Composition supplies session-aware repositories and its local clock. No feature Koin lookup. */
fun createShopDetailEntry(
    detailsRepository: ShopDetailsRepository,
    voteRepository: ShopCheckInVoteRepository,
    favoritesRepository: FavoritesRepository,
    currentViewer: suspend () -> Result<ShopViewer>,
    currentDayOfWeek: () -> Int,
    currentMinuteOfDay: () -> Int,
): ShopDetailEntry = DefaultShopDetailEntry(detailsRepository, voteRepository, favoritesRepository,
    currentViewer, currentDayOfWeek, currentMinuteOfDay)

private class DefaultShopDetailEntry(
    private val detailsRepository: ShopDetailsRepository,
    private val voteRepository: ShopCheckInVoteRepository,
    private val favoritesRepository: FavoritesRepository,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    private val currentDayOfWeek: () -> Int,
    private val currentMinuteOfDay: () -> Int,
) : ShopDetailEntry {
    @Composable
    override fun Content(
        shopId: String,
        sessionGeneration: Int,
        forCheckIn: Boolean,
        formatCheckInDate: (String) -> String,
        formatMenuDate: (String) -> String,
        formatDistance: (Double, Double) -> String?,
        onEffect: (ShopDetailEffect) -> Unit,
        refreshGeneration: Long,
    ) {
        val model = viewModel(key = "shop-detail:$shopId") {
            ShopDetailViewModel(shopId = shopId, detailsRepository = detailsRepository,
                checkInVoteRepository = voteRepository, favoritesRepository = favoritesRepository,
                currentViewer = currentViewer, currentDayOfWeek = currentDayOfWeek,
                currentMinuteOfDay = currentMinuteOfDay, initialSessionGeneration = sessionGeneration,
                initialRefreshGeneration = refreshGeneration)
        }
        ShopDetailScreen(viewModel = model, sessionGeneration = sessionGeneration,
            formatDistance = formatDistance, forCheckIn = forCheckIn,
            formatCheckInDate = formatCheckInDate, formatMenuDate = formatMenuDate,
            refreshGeneration = refreshGeneration, onEvent = { event ->
                event.toPublicEffect()?.let(onEffect)
            })
    }
}

private fun ShopDetailEvent.toPublicEffect(): ShopDetailEffect? = when (this) {
    ShopDetailEvent.Back -> ShopDetailEffect.Back
    ShopDetailEvent.ChooseAnotherShop -> ShopDetailEffect.ChooseAnotherShop
    ShopDetailEvent.SignIn -> ShopDetailEffect.SignIn
    ShopDetailEvent.Register -> ShopDetailEffect.Register
    is ShopDetailEvent.OpenMenuGallery -> ShopDetailEffect.OpenMenuGallery(shopId)
    is ShopDetailEvent.CreateCheckIn -> ShopDetailEffect.CreateCheckIn(shop)
    is ShopDetailEvent.ReportShop -> ShopDetailEffect.ReportShop(shop)
    is ShopDetailEvent.OpenCheckIns -> ShopDetailEffect.OpenCheckIns(shopId)
    is ShopDetailEvent.ReportCheckIn -> ShopDetailEffect.ReportCheckIn(checkInId)
    is ShopDetailEvent.FavoriteChanged -> ShopDetailEffect.FavoriteChanged(shopId, isFavorite)
    is ShopDetailEvent.OpenMap -> ShopDetailEffect.OpenMap(latitude, longitude, title)
    is ShopDetailEvent.OpenRoute -> ShopDetailEffect.OpenRoute(latitude, longitude)
    is ShopDetailEvent.ShareShop -> ShopDetailEffect.ShareShop(canonicalPath, title)
    is ShopDetailEvent.SuggestChange -> ShopDetailEffect.SuggestChange(shop)
    is ShopDetailEvent.OpenPhoto -> ShopDetailEffect.OpenPhoto(urls, index)
    is ShopDetailEvent.OpenRoaster -> ShopDetailEffect.OpenRoaster(id)
    is ShopDetailEvent.OpenLink -> ShopDetailEffect.OpenLink(target)
    is ShopDetailEvent.CopyPhone -> ShopDetailEffect.CopyPhone(number)
    // The runtime screen presents these feature-owned localized messages.
    ShopDetailEvent.VoteFailed, ShopDetailEvent.FavoriteFailed -> null
}
