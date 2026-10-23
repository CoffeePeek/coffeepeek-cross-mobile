package com.coffeepeek.feature.shop.impl

import com.coffeepeek.feature.shop.api.ShopAnalytics
import com.coffeepeek.feature.shop.impl.ui.data.LocalShopAnalytics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.shop.impl.ui.compose.component.LocalShopAuthorImage
import com.coffeepeek.feature.shop.impl.ui.compose.component.LocalShopCheckInImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shop.domain.repository.ShopFavorites
import com.coffeepeek.feature.shop.api.ShopDetailEffect
import com.coffeepeek.feature.shop.api.ShopDetailEntry
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.ShopDetailViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.ShopDetailScreen
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_author_unknown
import org.jetbrains.compose.resources.stringResource

/** Composition supplies session-aware repositories and its local clock. No feature Koin lookup. */
fun createShopDetailEntry(
    detailsRepository: ShopDetailsRepository,
    voteRepository: ShopCheckInVoteRepository,
    favoritesRepository: ShopFavorites,
    currentViewer: suspend () -> Result<ShopViewer>,
    currentDayOfWeek: () -> Int,
    currentMinuteOfDay: () -> Int,
    authorImage: (@Composable (String, Modifier) -> Unit)? = null,
    checkInImage: (@Composable (String, String?, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit)? = null,
    analytics: ShopAnalytics = ShopAnalytics.None,
): ShopDetailEntry = DefaultShopDetailEntry(detailsRepository, voteRepository, favoritesRepository,
    currentViewer, currentDayOfWeek, currentMinuteOfDay, authorImage, checkInImage, analytics)

private class DefaultShopDetailEntry(
    private val detailsRepository: ShopDetailsRepository,
    private val voteRepository: ShopCheckInVoteRepository,
    private val favoritesRepository: ShopFavorites,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    private val currentDayOfWeek: () -> Int,
    private val currentMinuteOfDay: () -> Int,
    private val authorImage: (@Composable (String, Modifier) -> Unit)?,
    private val checkInImage: (@Composable (String, String?, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit)?,
    private val analytics: ShopAnalytics,
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
        val unknownAuthor = stringResource(Res.string.shop_checkins_author_unknown)
        val model = viewModel(key = "shop-detail:$shopId") {
            ShopDetailViewModel(shopId = shopId, detailsRepository = detailsRepository,
                checkInVoteRepository = voteRepository, favoritesRepository = favoritesRepository,
                currentViewer = currentViewer, currentDayOfWeek = currentDayOfWeek,
                currentMinuteOfDay = currentMinuteOfDay, initialSessionGeneration = sessionGeneration,
                initialRefreshGeneration = refreshGeneration)
        }
        CompositionLocalProvider(LocalShopAnalytics provides analytics, LocalShopAuthorImage provides authorImage, LocalShopCheckInImage provides checkInImage) {
            ShopDetailScreen(viewModel = model, sessionGeneration = sessionGeneration,
                formatDistance = formatDistance, forCheckIn = forCheckIn,
                formatCheckInDate = formatCheckInDate, formatMenuDate = formatMenuDate,
                refreshGeneration = refreshGeneration, onEvent = { event ->
                    event.toPublicEffect(unknownAuthor)?.let(onEffect)
                })
        }
    }
}

private fun ShopDetailEvent.toPublicEffect(unknownAuthor: String): ShopDetailEffect? = when (this) {
    ShopDetailEvent.Back -> ShopDetailEffect.Back
    ShopDetailEvent.ChooseAnotherShop -> ShopDetailEffect.ChooseAnotherShop
    ShopDetailEvent.SignIn -> ShopDetailEffect.SignIn
    is ShopDetailEvent.FavoriteRequiresSignIn -> ShopDetailEffect.FavoriteRequiresSignIn(shopId, desired)
    ShopDetailEvent.Register -> ShopDetailEffect.Register
    is ShopDetailEvent.OpenMenuGallery -> ShopDetailEffect.OpenMenuGallery(shopId)
    is ShopDetailEvent.CreateCheckIn -> ShopDetailEffect.CreateCheckIn(shop)
    is ShopDetailEvent.ReportShop -> ShopDetailEffect.ReportShop(shop)
    is ShopDetailEvent.OpenCheckIns -> ShopDetailEffect.OpenCheckIns(shopId)
    is ShopDetailEvent.ReportCheckIn -> ShopDetailEffect.ReportCheckIn(checkInId)
    is ShopDetailEvent.OpenAuthor -> ShopDetailEffect.OpenAuthor(slug, nickname, avatarUrl)
    is ShopDetailEvent.BlockAuthor -> ShopDetailEffect.BlockAuthor(author.copy(nickname = author.nickname.ifBlank { unknownAuthor }))
    is ShopDetailEvent.FavoriteChanged -> ShopDetailEffect.FavoriteChanged(shopId, isFavorite)
    is ShopDetailEvent.OpenMap -> ShopDetailEffect.OpenMap(latitude, longitude, title)
    is ShopDetailEvent.OpenRoute -> ShopDetailEffect.OpenRoute(latitude, longitude)
    is ShopDetailEvent.ShareShop -> ShopDetailEffect.ShareShop(canonicalPath, title)
    is ShopDetailEvent.SuggestChange -> ShopDetailEffect.SuggestChange(shop)
    is ShopDetailEvent.OpenPhoto -> ShopDetailEffect.OpenPhoto(urls, index, originals)
    is ShopDetailEvent.OpenRoaster -> ShopDetailEffect.OpenRoaster(id)
    is ShopDetailEvent.OpenLink -> ShopDetailEffect.OpenLink(target)
    is ShopDetailEvent.CopyPhone -> ShopDetailEffect.CopyPhone(number)
    // The runtime screen presents these feature-owned localized messages.
    ShopDetailEvent.VoteFailed, ShopDetailEvent.FavoriteFailed -> null
}
