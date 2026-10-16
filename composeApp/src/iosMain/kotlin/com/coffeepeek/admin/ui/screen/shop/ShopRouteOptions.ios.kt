package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.component.NativeActionSheet
import com.coffeepeek.admin.ui.component.PlatformAction
import com.coffeepeek.admin.utils.OpenInBrowser

@Composable
internal actual fun ShopRoutePicker(latitude: Double, longitude: Double, onDismiss: () -> Unit) {
    NativeActionSheet(
        title = "Построить маршрут",
        actions = shopRouteOptions(latitude, longitude).map { option ->
            PlatformAction(option.label, onClick = { OpenInBrowser.openInBrowser(option.url) })
        },
        onDismiss = onDismiss,
    )
}

internal actual fun shopRouteOptions(latitude: Double, longitude: Double): List<ShopRouteOption> = listOf(
    ShopRouteOption("Apple Карты", buildAppleMapsRouteUrl(latitude, longitude)),
    ShopRouteOption("Google Карты", buildGoogleMapsRouteUrl(latitude, longitude)),
    ShopRouteOption("Яндекс Карты", buildYandexMapsRouteUrl(latitude, longitude)),
)
