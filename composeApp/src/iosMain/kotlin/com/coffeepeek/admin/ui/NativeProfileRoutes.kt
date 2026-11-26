package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.di.profile.iosEditProfileContent
import com.coffeepeek.admin.di.profile.iosGuestProfileContent
import com.coffeepeek.admin.di.profile.iosOwnProfileContent
import com.coffeepeek.admin.di.profile.iosPublicProfileContent
import platform.UIKit.UIViewController

@Composable
internal fun nativeProfileRouteContent(isLoggedIn: Boolean): (Navigator.Screen) -> (() -> UIViewController)? {
    val own = if (isLoggedIn) iosOwnProfileContent() else iosGuestProfileContent()
    val public = iosPublicProfileContent()
    val edit = iosEditProfileContent()
    return { route -> when (route) {
        Navigator.Screen.ProfileTab -> own
        Navigator.Screen.EditProfile -> edit
        is Navigator.Screen.UserProfile -> { { public(route.slug, route.nickname, route.avatarUrl, false) } }
        is Navigator.Screen.UserCheckIns -> { { public(route.slug, route.nickname, route.avatarUrl, true) } }
        else -> null
    } }
}
