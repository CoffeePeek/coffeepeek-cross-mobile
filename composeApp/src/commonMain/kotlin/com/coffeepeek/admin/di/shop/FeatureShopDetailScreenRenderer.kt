package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.coffeepeek.core.designsystem.component.coffeePeekViewportBottomPadding
import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.shop_share_named
import coffeepeek.composeapp.generated.resources.shop_phone_copied
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInFormRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopRoutePicker
import com.coffeepeek.admin.utils.ClipboardHelper
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.currentLocalDayOfWeek
import com.coffeepeek.admin.utils.currentLocalMinuteOfDay
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.domain.model.ShopLocation
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.api.ShopDetailContext
import com.coffeepeek.feature.shop.api.ShopDetailEffect
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.createShopDetailEntry
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString

/** Feature effects are adapted to the existing application destinations/platform services, never vice versa. */
internal class FeatureShopDetailScreenRenderer(
    private val sessions: SessionReader,
    private val accounts: ProfileAccountPort,
    private val details: ShopDetailsRepository,
    private val votes: ShopCheckInVoteRepository,
    private val favorites: FavoritesRepository,
    private val favoriteIntent: ShopFavoriteIntent,
    private val checkInForm: ShopCheckInFormRenderer,
    private val visibility: com.coffeepeek.feature.authorblocking.api.AuthorVisibility,
    private val blockActions: com.coffeepeek.feature.authorblocking.api.AuthorBlockActions,
) : ShopDetailScreenRenderer {
    @Composable
    override fun Content(shopId: String, forCheckIn: Boolean) {
        ShopBrowsingHost("shop-detail:$shopId", sessions, visibility) { session ->
            val entry = remember(session) { createEntry(session) }
            val state = rememberShopDetailHostState()
            DetailContent(entry, state, session, shopId, forCheckIn)
            Overlays(state, session)
        }
    }

    private fun createEntry(session: ShopBrowsingHostSession) = createShopDetailEntry(
        session.lease.details(details), session.lease.votes(votes),
        shopFavorites(session.lease.favorites(favorites)), currentViewer = { session.lease.viewer(accounts) },
        currentDayOfWeek = ::currentLocalDayOfWeek, currentMinuteOfDay = ::currentLocalMinuteOfDay,
        analytics = AppShopAnalytics,
        authorImage = { url, modifier -> com.coffeepeek.core.designsystem.component.CoffeePeekImage(data = url, modifier = modifier) },
        checkInImage = { url, original, modifier, scale ->
            com.coffeepeek.core.designsystem.component.CoffeePeekImage(data = url, highQualityData = original, modifier = modifier, contentScale = scale)
        },
    )

    @Composable
    private fun DetailContent(
        entry: com.coffeepeek.feature.shop.api.ShopDetailEntry, state: ShopDetailHostState,
        session: ShopBrowsingHostSession, shopId: String, forCheckIn: Boolean,
    ) {
        val location = rememberPermittedUserLocation()
        val formatDate = rememberCheckInDateLabel()
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val copied = stringResource(Res.string.shop_phone_copied)
        Box(Modifier.fillMaxSize()) {
            entry.Content(shopId = shopId, sessionGeneration = session.generation, forCheckIn = forCheckIn,
                formatCheckInDate = formatDate, formatMenuDate = ::formatShopMenuLocalDate,
                formatDistance = { lat, lon -> formatDistance(distanceToShopMeters(location,
                    ShopLocation(latitude = lat, longitude = lon))) },
                refreshGeneration = state.refreshGeneration,
                onEffect = { if (session.lease.isCurrent()) handleEffect(it, state, session, shopId, scope, snackbar, copied) })
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)
                .coffeePeekViewportBottomPadding().coffeePeekScreenBottomPadding().padding(bottom = 72.dp))
        }
    }

    @Composable
    private fun Overlays(state: ShopDetailHostState, session: ShopBrowsingHostSession) {
        state.preview?.let { photo ->
            CoffeePeekPhotoViewer(images = com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages(photo.imageUrls, photo.originalUrls),
                initialIndex = photo.initialIndex, onDismiss = { state.preview = null })
        }
        state.route?.let { ShopRoutePicker(it.latitude, it.longitude, onDismiss = { state.route = null }) }
        state.form?.let { shop ->
            checkInForm.Content(shopId = shop.shopId, shopSlug = shop.shopSlug, shopName = shop.shopName,
                onDismiss = { state.form = null },
                onSubmitted = { if (session.lease.isCurrent()) state.refreshGeneration++ },
                onGoToFeed = {
                    state.form = null
                    Navigator.popThenSelectTab(Navigator.Screen.CommunityTab)
                },
                onOpenHistory = {
                    state.form = null
                    Navigator.popThenNavigate(Navigator.Screen.VisitedPlaces)
                },
                legacyContent = {})
        }
    }

    private fun handleEffect(
        effect: ShopDetailEffect, state: ShopDetailHostState, session: ShopBrowsingHostSession, shopId: String,
        scope: kotlinx.coroutines.CoroutineScope, snackbar: SnackbarHostState, copied: String,
    ) {
        when (effect) {
            ShopDetailEffect.Back -> Navigator.popBack()
            ShopDetailEffect.ChooseAnotherShop -> Navigator.popThenNavigate(Navigator.Screen.CreateCheckIn)
            ShopDetailEffect.SignIn -> Navigator.navigate(Navigator.Screen.Auth)
            ShopDetailEffect.Register -> Navigator.navigate(Navigator.Screen.Register)
            is ShopDetailEffect.OpenMenuGallery -> Navigator.navigate(Navigator.Screen.ShopMenuGallery(effect.shopId))
            is ShopDetailEffect.CreateCheckIn -> state.form = effect.shop
            is ShopDetailEffect.ReportShop -> Navigator.navigate(Navigator.Screen.ReportShop(effect.shop.shopId, effect.shop.shopName))
            is ShopDetailEffect.OpenCheckIns -> Navigator.navigate(Navigator.Screen.ShopReviews(effect.shopId))
            is ShopDetailEffect.ReportCheckIn -> Navigator.navigate(Navigator.Screen.ReportCheckIn(effect.checkInId))
            is ShopDetailEffect.OpenAuthor -> Navigator.navigate(Navigator.Screen.UserProfile(effect.slug, effect.nickname, effect.avatarUrl))
            is ShopDetailEffect.OpenMap -> Navigator.openShopOnMap(shopId, effect.latitude, effect.longitude, effect.title)
            is ShopDetailEffect.OpenRoute -> state.route = effect
            is ShopDetailEffect.SuggestChange -> Navigator.navigate(Navigator.Screen.SuggestShopChange(effect.shop.shopId))
            is ShopDetailEffect.OpenPhoto -> state.preview = effect
            is ShopDetailEffect.OpenRoaster -> Navigator.navigate(Navigator.Screen.RoasterDetail(effect.slug))
            else -> platformEffect(effect, session, scope, snackbar, copied)
        }
    }

    private fun platformEffect(
        effect: ShopDetailEffect, session: ShopBrowsingHostSession,
        scope: kotlinx.coroutines.CoroutineScope, snackbar: SnackbarHostState, copied: String,
    ) {
        when (effect) {
            is ShopDetailEffect.FavoriteRequiresSignIn -> {
                favoriteIntent.remember(effect.shopId, effect.desired)
                Navigator.navigate(Navigator.Screen.Auth)
            }
            is ShopDetailEffect.BlockAuthor -> session.lease.requestAuthorBlock(effect.author, blockActions)
            is ShopDetailEffect.FavoriteChanged -> scope.launch {
                if (session.lease.isCurrent()) FavoriteSync.notifyChanged(effect.shopId, effect.isFavorite)
            }
            is ShopDetailEffect.ShareShop -> scope.launch { share(effect, session) }
            is ShopDetailEffect.OpenLink -> OpenInBrowser.openInBrowser(effect.target)
            is ShopDetailEffect.CopyPhone -> {
                ClipboardHelper.copyText(effect.number)
                scope.launch { snackbar.showSnackbar(copied) }
            }
            else -> Unit
        }
    }

    private suspend fun share(effect: ShopDetailEffect.ShareShop, session: ShopBrowsingHostSession) {
        val text = getString(Res.string.shop_share_named, effect.title, "https://coffeepeek.by${effect.canonicalPath}")
        if (session.lease.isCurrent()) ShareHelper.shareText(text)
    }
}

private class ShopDetailHostState(
    private val savedForm: androidx.compose.runtime.MutableState<ShopDetailContext?>,
    private val savedRefresh: androidx.compose.runtime.MutableLongState,
) {
    var form by savedForm
    var refreshGeneration by savedRefresh
    var preview by mutableStateOf<ShopDetailEffect.OpenPhoto?>(null)
    var route by mutableStateOf<ShopDetailEffect.OpenRoute?>(null)
}

@Composable
private fun rememberShopDetailHostState(): ShopDetailHostState {
    val form = rememberSaveable(stateSaver = shopFormContextSaver) { mutableStateOf<ShopDetailContext?>(null) }
    val refresh = rememberSaveable { mutableLongStateOf(0) }
    return remember(form, refresh) { ShopDetailHostState(form, refresh) }
}

internal fun formatShopMenuLocalDate(raw: String): String {
    val date = utcIsoToLocalDate(raw)
    val parts = date.split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
}

private val shopFormContextSaver = listSaver<ShopDetailContext?, String>(
    save = { shop -> shop?.let { listOf(it.shopId, it.shopSlug, it.shopName) }.orEmpty() },
    restore = { values -> values.takeIf { it.size == 3 }?.let { ShopDetailContext(it[0], it[1], it[2]) } },
)
