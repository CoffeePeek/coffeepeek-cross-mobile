package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.di.checkinhistory.iosHomeCheckInContent
import platform.UIKit.UIViewController

@Composable
internal fun nativeHomeCheckInRouteContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val home = iosHomeCheckInContent()
    return { route -> when (route) {
        is Navigator.Screen.HomeCheckInDetail -> { { home(route.id, false) } }
        is Navigator.Screen.EditHomeCheckIn -> { { home(route.id, true) } }
        else -> null
    } }
}
