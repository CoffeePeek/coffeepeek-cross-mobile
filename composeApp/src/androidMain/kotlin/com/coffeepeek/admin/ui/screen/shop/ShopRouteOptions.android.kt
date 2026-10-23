package com.coffeepeek.admin.ui.screen.shop

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.coffeepeek.admin.utils.ErrorHandler

@Composable
internal actual fun ShopRoutePicker(latitude: Double, longitude: Double, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dismiss = rememberUpdatedState(onDismiss)
    LaunchedEffect(latitude, longitude, context) {
        val destination = Intent(Intent.ACTION_VIEW, "geo:0,0?q=$latitude,$longitude".toUri())
        val webRoute = Intent(Intent.ACTION_VIEW, buildGoogleMapsRouteUrl(latitude, longitude).toUri())
        val chooser = if (destination.resolveActivity(context.packageManager) != null) {
            // Prefer the providers' directions payloads; other installed maps receive the destination.
            // Android merges matches for the same target component, preferring earlier payloads.
            Intent.createChooser(
                Intent(webRoute).setPackage("com.google.android.apps.maps"),
                "Построить маршрут",
            ).putExtra(
                Intent.EXTRA_ALTERNATE_INTENTS,
                arrayOf(
                    Intent(Intent.ACTION_VIEW, buildYandexMapsRouteUrl(latitude, longitude).toUri())
                        .setPackage("ru.yandex.yandexmaps"),
                    destination,
                ),
            )
        } else {
            Intent.createChooser(webRoute, "Построить маршрут")
        }
        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            ErrorHandler.showError("Не удалось открыть приложение для маршрута")
        } catch (_: SecurityException) {
            ErrorHandler.showError("Не удалось открыть приложение для маршрута")
        } finally {
            dismiss.value()
        }
    }
}

internal data class ShopRouteOption(val label: String, val url: String)

internal fun shopRouteOptions(latitude: Double, longitude: Double): List<ShopRouteOption> = listOf(
    ShopRouteOption("Google Карты", buildGoogleMapsRouteUrl(latitude, longitude)),
    ShopRouteOption("Яндекс Карты", buildYandexMapsRouteUrl(latitude, longitude)),
)
