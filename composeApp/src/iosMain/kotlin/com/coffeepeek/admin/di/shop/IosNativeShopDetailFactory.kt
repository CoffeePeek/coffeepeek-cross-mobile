package com.coffeepeek.admin.di.shop

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.ownership.iosOwnershipActionsController
import com.coffeepeek.admin.location.*
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.utils.*
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.*
import com.coffeepeek.feature.shop.domain.repository.*
import com.coffeepeek.feature.shop.impl.createNativeShopDetailSession
import com.coffeepeek.feature.shop.impl.NativeShopLinkedCheckIn
import com.coffeepeek.feature.shop.impl.ui.NativeShopDetailIosSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString
import platform.UIKit.UIViewController

internal class IosNativeShopDetailFactory(private val accounts: SessionReader, private val profiles: ProfileAccountPort,
    private val visibility: AuthorVisibility, private val details: ShopDetailsRepository, private val votes: ShopCheckInVoteRepository,
    private val favorites: FavoritesRepository, private val favoriteIntent: ShopFavoriteIntent,
    private val collections: FavoriteCollectionsPrompt?, private val blocks: AuthorBlockActions,
    private val forms: SessionShopCheckInEntry, private val language: LanguagePort, private val fetcher: ImageUrlFetcher,
    private val ownership: BusinessOwnershipEntry,
) {
    fun controller(id: String, openCheckIn: Boolean): UIViewController =
        IosShopDetailControllers.controller(IosShopDetailRoute(this, accounts, visibility, language, id, openCheckIn))

    internal suspend fun bind(route: IosShopDetailRoute, session: ShopBrowsingHostSession, id: String,
        linkedCheckIn: NativeShopLinkedCheckIn,
    ) = coroutineScope {
        val core = createNativeShopDetailSession(id, session.lease.details(details), session.lease.votes(votes),
            shopFavorites(session.lease.favorites(favorites)), { session.lease.viewer(profiles) }, ::currentLocalDayOfWeek,
            ::currentLocalMinuteOfDay, session.generation, collections != null, session.lease::isCurrent, AppShopAnalytics, session.viewModelStore)
        val dates = NativeReviewDateFormatter(this, language.observe().map { it.languageTag })
        val platformScope = this
        var location: GeoPoint? = null
        var overlays: IosShopDetailOverlays? = null
        lateinit var binding: NativeShopDetailIosSession
        binding = NativeShopDetailIosSession(core, dates.languages, dates::format,
            ::formatShopMenuLocalDate, { lat, lon -> formatDistance(distanceToShopMeters(location, lat, lon)) }, AppShopAnalytics,
            { event -> if (session.lease.isCurrent()) effect(event, session, id, checkNotNull(overlays), binding, platformScope) }, linkedCheckIn)
        bindSource(route, session, binding, { source -> overlays = source.overlays }, {
            if (PlatformLocation.hasPermission()) location = PlatformLocation.getLastKnownLocation()
            binding.formatChanged()
        })
    }

    private suspend fun bindSource(route: IosShopDetailRoute, session: ShopBrowsingHostSession, binding: NativeShopDetailIosSession,
        attached: (IosShopDetail) -> Unit, location: suspend () -> Unit,
    ) = coroutineScope {
        var source: IosShopDetail? = null
        try {
            val initial = binding.state.filterNotNull().first()
            if (!session.lease.isCurrent()) return@coroutineScope
            val detail = createSource(initial, session, binding)
            source = detail
            attached(detail)
            route.update(detail)
            observeSource(detail, binding, location)
        } finally {
            source?.revoke()
            binding.close()
            route.update(null)
            coroutineContext.cancelChildren()
        }
    }

    private suspend fun observeSource(detail: IosShopDetail, binding: NativeShopDetailIosSession,
        location: suspend () -> Unit,
    ): Nothing = coroutineScope {
        launch { binding.state.filterNotNull().collect(detail::update) }
        launch { location() }
        launch { ownership.profileChanges.drop(1).collect { binding.core.retry() } }
        analytics(detail, binding, this)
        awaitCancellation()
    }

    private fun createSource(initial: com.coffeepeek.feature.shop.impl.ui.ShopDetailIosPresentation,
        session: ShopBrowsingHostSession, binding: NativeShopDetailIosSession,
    ): IosShopDetail {
        val media = IosCatalogueMedia(fetcher, Constants.BASE_URL)
        val detail = IosShopDetail(initial, media, session.lease) { id, slug, allowed, suggest, edit ->
            iosOwnershipActionsController(ownership, language, media, "CoffeeShop", id, slug, allowed, suggest, edit) }
        var refresh = 0L
        val overlays = IosShopDetailOverlays(detail.scope, session.lease::isCurrent, accounts, language, fetcher, forms) {
            binding.core.refresh(++refresh)
        }
        detail.bindOwner(overlays, binding::visibility, binding::close)
        return detail
    }

    private fun analytics(source: IosShopDetail, binding: NativeShopDetailIosSession, scope: CoroutineScope) {
        val tracker = NativeScreenAnalytics(scope, "shop_detail", source.screenVisible)
        tracker.entity(Entity.Shop, binding.state.map { it?.state?.details != null && binding.core.isCurrent() })
        ShopExposure.entries.forEach { feature -> tracker.feature(feature.wire(), "shop_detail",
            combine(source.exposures, binding.state) { exposures, state ->
                feature.name in exposures && state?.state?.priceInfoVisible == false && binding.core.isCurrent()
            }) }
        val key = correlationId("shop-price")
        scope.launch { try { binding.state.collect { Analytics.service?.overlay(key, it?.state?.priceInfoVisible == true) } }
            finally { Analytics.service?.overlay(key, false) } }
    }

    private fun effect(effect: ShopDetailEffect, session: ShopBrowsingHostSession, id: String,
        overlays: IosShopDetailOverlays, binding: NativeShopDetailIosSession, scope: CoroutineScope,
    ) {
        if (navigateShopEffect(effect)) return
        when (effect) {
            ShopDetailEffect.Back -> Navigator.popBack()
            is ShopDetailEffect.CreateCheckIn -> overlays.editor(effect.shop)
            is ShopDetailEffect.OpenPhoto -> overlays.photo(effect)
            is ShopDetailEffect.OpenRoute -> overlays.route(effect)
            is ShopDetailEffect.OpenMap -> Navigator.openShopOnMap(id, effect.latitude, effect.longitude, effect.title)
            is ShopDetailEffect.SelectFavoriteCollections -> collections?.shop(effect.shopId)
            is ShopDetailEffect.FavoriteRequiresSignIn -> {
                favoriteIntent.remember(effect.shopId, effect.desired)
                Navigator.navigate(Navigator.Screen.Auth)
            }
            is ShopDetailEffect.BlockAuthor -> session.lease.requestAuthorBlock(effect.author, blocks)
            else -> platformEffect(effect, session, binding, scope)
        }
    }

    private fun platformEffect(effect: ShopDetailEffect, session: ShopBrowsingHostSession,
        binding: NativeShopDetailIosSession, scope: CoroutineScope,
    ) {
        when (effect) {
            is ShopDetailEffect.FavoriteChanged -> scope.launch { if (session.lease.isCurrent()) FavoriteSync.notifyChanged(effect.shopId, effect.isFavorite) }
            is ShopDetailEffect.OpenLink -> OpenInBrowser.openInBrowser(effect.target)
            is ShopDetailEffect.CopyPhone -> {
                ClipboardHelper.copyText(effect.number)
                scope.launch { binding.showMessage(getString(Res.string.shop_phone_copied)) }
            }
            is ShopDetailEffect.ShareShop -> scope.launch {
                val text = getString(Res.string.shop_share_named, effect.title, "https://coffeepeek.by${effect.canonicalPath}")
                if (session.lease.isCurrent()) ShareHelper.shareText(text)
            }
            else -> Unit
        }
    }

}

private fun navigateShopEffect(effect: ShopDetailEffect): Boolean {
    val destination = shopEffectDestination(effect) ?: return false
    Navigator.navigate(destination)
    return true
}

private fun shopEffectDestination(effect: ShopDetailEffect): Navigator.Screen? = when (effect) {
    ShopDetailEffect.SignIn -> Navigator.Screen.Auth
    ShopDetailEffect.Register -> Navigator.Screen.Register
    is ShopDetailEffect.OpenMenuGallery -> Navigator.Screen.ShopMenuGallery(effect.shopId)
    is ShopDetailEffect.ReportShop -> Navigator.Screen.ReportShop(effect.shop.shopId, effect.shop.shopName)
    is ShopDetailEffect.OpenCheckIns -> Navigator.Screen.ShopReviews(effect.shopId)
    is ShopDetailEffect.ReportCheckIn -> Navigator.Screen.ReportCheckIn(effect.checkInId)
    is ShopDetailEffect.OpenAuthor -> Navigator.Screen.UserProfile(effect.slug, effect.nickname, effect.avatarUrl)
    is ShopDetailEffect.SuggestChange -> Navigator.Screen.SuggestShopChange(effect.shop.shopId)
    is ShopDetailEffect.OpenRoaster -> Navigator.Screen.RoasterDetail(effect.slug)
    else -> null
}
