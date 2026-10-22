package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.ui.nativeScreenHost

@Composable
internal actual fun NativeNavigationBar(title: String, onBack: (() -> Unit)?, customBack: Boolean, action: NavigationBarAction?): Boolean {
    val host = LocalUIViewController.current.nativeScreenHost() ?: return false
    DisposableEffect(host, customBack, onBack != null) {
        host.handlesBackLocally = customBack && onBack != null
        onDispose { host.handlesBackLocally = false }
    }
    // Keep UIKit's edge-back policy while the screen renders its Compose header.
    return false
}
