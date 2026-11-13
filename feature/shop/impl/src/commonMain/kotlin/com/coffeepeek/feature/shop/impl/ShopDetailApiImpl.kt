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
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

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
    selectCollectionsOnFavorite: Boolean = false,
): ShopDetailEntry = DefaultShopDetailEntry(detailsRepository, voteRepository, favoritesRepository,
    currentViewer, currentDayOfWeek, currentMinuteOfDay, authorImage, checkInImage, analytics, selectCollectionsOnFavorite)

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
    private val selectCollectionsOnFavorite: Boolean,
) : ShopDetailEntry {
    @Composable
    override fun Content(
        shopId: String,
        sessionGeneration: Int,
        formatCheckInDate: (String) -> String,
        formatMenuDate: (String) -> String,
        formatDistance: (Double, Double) -> String?,
        onEffect: (ShopDetailEffect) -> Unit,
        refreshGeneration: Long,
        openCheckIn: Boolean,
    ) {
        val unknownAuthor = stringResource(Res.string.shop_checkins_author_unknown)
        val model = viewModel(key = "shop-detail:$shopId") {
            ShopDetailViewModel(shopId = shopId, detailsRepository = detailsRepository,
                checkInVoteRepository = voteRepository, favoritesRepository = favoritesRepository,
                currentViewer = currentViewer, currentDayOfWeek = currentDayOfWeek,
                currentMinuteOfDay = currentMinuteOfDay, initialSessionGeneration = sessionGeneration,
                initialRefreshGeneration = refreshGeneration, selectCollectionsOnFavorite = selectCollectionsOnFavorite)
        }
        CompositionLocalProvider(LocalShopAnalytics provides analytics, LocalShopAuthorImage provides authorImage, LocalShopCheckInImage provides checkInImage) {
            ShopDetailScreen(viewModel = model, sessionGeneration = sessionGeneration,
                formatDistance = formatDistance,
                formatCheckInDate = formatCheckInDate, formatMenuDate = formatMenuDate,
                refreshGeneration = refreshGeneration, openCheckIn = openCheckIn, onEvent = { event ->
                    event.toPublicEffect(unknownAuthor)?.let(onEffect)
                })
        }
    }
}

private fun ShopDetailEvent.toPublicEffect(unknownAuthor: String): ShopDetailEffect? = when (this) {
    ShopDetailEvent.Back -> ShopDetailEffect.Back
    ShopDetailEvent.SignIn -> ShopDetailEffect.SignIn
    is ShopDetailEvent.FavoriteRequiresSignIn -> ShopDetailEffect.FavoriteRequiresSignIn(shopId, desired)
    is ShopDetailEvent.SelectFavoriteCollections -> ShopDetailEffect.SelectFavoriteCollections(shopId)
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
