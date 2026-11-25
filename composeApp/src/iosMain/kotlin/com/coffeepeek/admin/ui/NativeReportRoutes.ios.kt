package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.di.contentreport.iosContentReportContent
import com.coffeepeek.admin.di.shopreport.iosShopReportContent
import platform.UIKit.UIViewController

@Composable
internal fun nativeReportRouteContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val shops = iosShopReportContent()
    val posts = iosContentReportContent()
    return { route -> when (route) {
        is Navigator.Screen.ReportShop -> shops(route.shopId, route.shopTitle)
        is Navigator.Screen.ReportCheckIn -> posts(route.checkInId, true, false)
        is Navigator.Screen.ReportReview -> posts(route.reviewId, false, route.isPreview)
        else -> null
    } }
}
