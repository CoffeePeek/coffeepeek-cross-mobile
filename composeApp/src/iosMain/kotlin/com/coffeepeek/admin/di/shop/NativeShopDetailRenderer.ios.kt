package com.coffeepeek.admin.di.shop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.coffeepeek.admin.analytics.TrackNativeFeatureExposure
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.ownership.iosOwnershipActionsController
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.ownership.api.LocalBusinessOwnership
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.ShopDetailIosPresentation
import com.coffeepeek.feature.shop.impl.ui.compose.configureIosShopDetail
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal fun configureNativeShopDetails(detail: (IosShopDetail) -> UIViewController,
    photo: (com.coffeepeek.admin.di.media.IosPhotoViewer, String, () -> Unit) -> UIViewController,
    message: (IosShopDetailNotice) -> UIViewController,
    error: (IosShopDetailSessionError) -> UIViewController) {
    configureNativeShopDetailOverlays(photo, message, error)
    configureIosShopDetail { data -> NativeShopDetail(data, detail) }
}

@Composable
private fun NativeShopDetail(data: ShopDetailIosPresentation, factory: (IosShopDetail) -> UIViewController) {
    val source = checkNotNull(LocalViewModelStoreOwner.current as? ShopBrowsingHostSession)
    val media = koinInject<ImageUrlFetcher>()
    val ownership = LocalBusinessOwnership.current
    val language = koinInject<LanguagePort>()
    val detail = remember(source, ownership, language) {
        IosShopDetail(data, IosCatalogueMedia(media, Constants.BASE_URL), source.lease,
            { id, slug, allowed, onSuggest, onEdit -> ownership?.let {
                iosOwnershipActionsController(it, language, "CoffeeShop", id, slug, allowed, onSuggest, onEdit)
            } })
    }
    val controller = remember(detail) { factory(detail) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { detail.update(data); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(detail) { onDispose { detail.close() } }
    val exposed by detail.exposures.collectAsState()
    ShopExposure.entries.filter { it.name in exposed }.forEach { feature ->
        key(feature) { TrackNativeFeatureExposure(!data.state.priceInfoVisible && source.lease.isCurrent(), feature.wire(), "shop_detail") }
    }
    NativeEditorControllerHost(controller, Modifier.fillMaxSize())
}
