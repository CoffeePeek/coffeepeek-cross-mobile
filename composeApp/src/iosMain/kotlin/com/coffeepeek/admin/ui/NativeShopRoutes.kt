package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.di.shop.iosShopDetailContent
import com.coffeepeek.admin.di.shop.iosShopCheckInListContent
import com.coffeepeek.admin.di.shop.iosShopMenuGalleryContent
import com.coffeepeek.admin.di.shop.iosShopCheckInContent
import platform.UIKit.UIViewController

@Composable
internal fun nativeShopRouteContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val detail = iosShopDetailContent()
    val checkIns = iosShopCheckInListContent()
    val gallery = iosShopMenuGalleryContent()
    val editor = iosShopCheckInContent()
    return { route -> when (route) {
        is Navigator.Screen.ShopDetail -> { { detail(route.shopId, route.openCheckIn) } }
        is Navigator.Screen.ShopReviews -> { { checkIns(route.shopId) } }
        is Navigator.Screen.ShopMenuGallery -> { { gallery(route.shopId) } }
        is Navigator.Screen.ShopCheckInEditor -> { { editor(route.shopId) } }
        else -> null
    } }
}
