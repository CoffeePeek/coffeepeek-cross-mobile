package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.route_choose_app
import coffeepeek.composeapp.generated.resources.route_apple_maps
import coffeepeek.composeapp.generated.resources.route_google_maps
import coffeepeek.composeapp.generated.resources.route_yandex_maps
import com.coffeepeek.admin.ui.component.NativeActionSheet
import com.coffeepeek.admin.ui.component.PlatformAction
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal actual fun ShopRoutePicker(latitude: Double, longitude: Double, onDismiss: () -> Unit) {
    NativeActionSheet(
        title = stringResource(Res.string.route_choose_app),
        actions = listOf(
            PlatformAction(stringResource(Res.string.route_apple_maps)) {
                OpenInBrowser.openInBrowser(buildAppleMapsRouteUrl(latitude, longitude))
            },
            PlatformAction(stringResource(Res.string.route_google_maps)) {
                OpenInBrowser.openInBrowser(buildGoogleMapsRouteUrl(latitude, longitude))
            },
            PlatformAction(stringResource(Res.string.route_yandex_maps)) {
                OpenInBrowser.openInBrowser(buildYandexMapsRouteUrl(latitude, longitude))
            },
        ),
        onDismiss = onDismiss,
    )
}
