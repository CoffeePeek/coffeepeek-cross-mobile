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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInFormRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.buildYandexMapsRouteUrl
import com.coffeepeek.admin.utils.ClipboardHelper
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.currentLocalDayOfWeek
import com.coffeepeek.admin.utils.currentLocalMinuteOfDay
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.domain.model.ShopLocation
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
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
internal class AndroidShopDetailScreenRenderer(
    private val sessions: SessionRepository,
    private val users: UserRepository,
    private val details: ShopDetailsRepository,
    private val votes: ShopCheckInVoteRepository,
    private val favorites: FavoritesRepository,
    private val checkInForm: ShopCheckInFormRenderer,
) : ShopDetailScreenRenderer {
    @Composable
    override fun Content(shopId: String, forCheckIn: Boolean) {
        AndroidShopBrowsingHost("android-shop-detail:$shopId", sessions) { session ->
            val entry = remember(session) {
                createShopDetailEntry(session.lease.details(details), session.lease.votes(votes),
                    shopFavorites(session.lease.favorites(favorites)), currentViewer = { session.lease.viewer(users) },
                    currentDayOfWeek = ::currentLocalDayOfWeek, currentMinuteOfDay = ::currentLocalMinuteOfDay)
            }
            val location = rememberPermittedUserLocation()
            val formatCheckInDate = rememberCheckInDateLabel()
            var form by rememberSaveable(stateSaver = shopFormContextSaver) { mutableStateOf<ShopDetailContext?>(null) }
            var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
            var refreshGeneration by rememberSaveable { mutableLongStateOf(0) }
            val scope = rememberCoroutineScope()
            val snackbar = remember { SnackbarHostState() }
            val copied = stringResource(Res.string.shop_phone_copied)
            Box(Modifier.fillMaxSize()) {
                entry.Content(shopId = shopId, sessionGeneration = session.generation, forCheckIn = forCheckIn,
                    formatCheckInDate = formatCheckInDate,
                    formatMenuDate = ::formatShopMenuLocalDate,
                    formatDistance = { latitude, longitude -> formatDistance(distanceToShopMeters(location,
                        ShopLocation(latitude = latitude, longitude = longitude))) },
                    refreshGeneration = refreshGeneration,
                    onEffect = effects@{ effect ->
                        if (!session.lease.isCurrent()) return@effects
                        when (effect) {
                            ShopDetailEffect.Back -> Navigator.popBack()
                            ShopDetailEffect.ChooseAnotherShop -> Navigator.popThenNavigate(Navigator.Screen.CreateCheckIn)
                            ShopDetailEffect.SignIn -> Navigator.navigate(Navigator.Screen.Auth)
                            ShopDetailEffect.Register -> Navigator.navigate(Navigator.Screen.Register)
                            is ShopDetailEffect.OpenMenuGallery -> Navigator.navigate(Navigator.Screen.ShopMenuGallery(effect.shopId))
                            is ShopDetailEffect.CreateCheckIn -> form = effect.shop
                            is ShopDetailEffect.ReportShop -> Navigator.navigate(Navigator.Screen.ReportShop(effect.shop.shopId, effect.shop.shopName))
                            is ShopDetailEffect.OpenCheckIns -> Navigator.navigate(Navigator.Screen.ShopReviews(effect.shopId))
                            is ShopDetailEffect.ReportCheckIn -> Navigator.navigate(Navigator.Screen.ReportCheckIn(effect.checkInId))
                            is ShopDetailEffect.OpenAuthor -> Navigator.navigate(Navigator.Screen.UserProfile(effect.slug, effect.nickname, effect.avatarUrl))
                            is ShopDetailEffect.FavoriteChanged -> scope.launch {
                                if (session.lease.isCurrent()) FavoriteSync.notifyChanged(effect.shopId, effect.isFavorite)
                            }
                            is ShopDetailEffect.OpenMap -> Navigator.openShopOnMap(shopId, effect.latitude, effect.longitude, effect.title)
                            is ShopDetailEffect.OpenRoute -> OpenInBrowser.openInBrowser(buildYandexMapsRouteUrl(effect.latitude, effect.longitude))
                            is ShopDetailEffect.ShareShop -> scope.launch {
                                val text = getString(Res.string.shop_share_named, effect.title,
                                    "https://coffeepeek.by${effect.canonicalPath}")
                                if (session.lease.isCurrent()) ShareHelper.shareText(text)
                            }
                            is ShopDetailEffect.SuggestChange -> Navigator.navigate(Navigator.Screen.SuggestShopChange(effect.shop.shopId))
                            is ShopDetailEffect.OpenPhoto -> preview = effect.imageUrls to effect.initialIndex
                            is ShopDetailEffect.OpenRoaster -> Navigator.navigate(Navigator.Screen.RoasterDetail(effect.slug))
                            is ShopDetailEffect.OpenLink -> OpenInBrowser.openInBrowser(effect.target)
                            is ShopDetailEffect.CopyPhone -> {
                                ClipboardHelper.copyText(effect.number)
                                scope.launch { snackbar.showSnackbar(copied) }
                            }
                        }
                    })
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp))
            }
            preview?.let { (urls, index) ->
                FullScreenImageDialog(imageUrls = urls, initialIndex = index, onDismiss = { preview = null })
            }
            form?.let { shop ->
                checkInForm.Content(shopId = shop.shopId, shopSlug = shop.shopSlug, shopName = shop.shopName,
                    onDismiss = { form = null },
                    onSubmitted = { if (session.lease.isCurrent()) refreshGeneration++ },
                    onGoToFeed = { form = null; Navigator.popThenSelectTab(Navigator.Screen.CommunityTab) },
                    onOpenHistory = { form = null; Navigator.popThenNavigate(Navigator.Screen.VisitedPlaces) },
                    legacyContent = {})
            }
        }
    }
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
