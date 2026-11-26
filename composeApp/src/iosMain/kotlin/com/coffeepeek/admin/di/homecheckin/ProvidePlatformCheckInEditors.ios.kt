package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.coffeepeek.admin.di.shop.IosShopCheckInEditor
import com.coffeepeek.admin.di.shop.LocalIosShopCheckInEditorController
import platform.UIKit.UIViewController

/** Application configuration only: sessions and controllers remain owned by their destinations. */
internal lateinit var homeEditorFactory: (IosHomeCheckInEditor) -> UIViewController
internal lateinit var shopEditorFactory: (IosShopCheckInEditor) -> UIViewController

internal fun configureNativeCheckInEditors(home: (IosHomeCheckInEditor) -> UIViewController,
    shop: (IosShopCheckInEditor) -> UIViewController) {
    homeEditorFactory = home
    shopEditorFactory = shop
}

@Composable
internal actual fun ProvidePlatformCheckInEditors(content: @Composable () -> Unit) {
    check(::homeEditorFactory.isInitialized && ::shopEditorFactory.isInitialized)
    CompositionLocalProvider(LocalIosHomeCheckInEditorController provides homeEditorFactory,
        LocalIosShopCheckInEditorController provides shopEditorFactory, content = content)
}
