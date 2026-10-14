package com.coffeepeek.admin

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.appLinkScreen
import platform.UIKit.UIViewController

private var koinInitialized = false

fun handleAppLink(link: String): Boolean {
    val screen = appLinkScreen(link) ?: return false
    Navigator.openAppLink(screen)
    return true
}

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    return ComposeUIViewController { App() }
}
