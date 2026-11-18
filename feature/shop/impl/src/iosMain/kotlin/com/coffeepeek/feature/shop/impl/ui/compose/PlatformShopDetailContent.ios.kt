package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.material3.SnackbarHostState
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.shop.api.ShopActionMetric
import com.coffeepeek.feature.shop.api.ShopContentMetric
import com.coffeepeek.feature.shop.api.ShopContentAction
import com.coffeepeek.feature.shop.impl.ui.*
import com.coffeepeek.feature.shop.impl.ui.compose.model.*
import com.coffeepeek.feature.shop.impl.ui.data.LocalShopAnalytics

private var presenter: (@Composable (ShopDetailIosPresentation) -> Unit)? = null

fun configureIosShopDetail(present: @Composable (ShopDetailIosPresentation) -> Unit) { presenter = present }

@Composable
internal actual fun PlatformShopDetailContent(
    state: ShopDetailState, onAction: (ShopDetailAction) -> Unit, distanceLabel: String?,
    formatCheckInDate: (String) -> String, formatMenuDate: (String) -> String, snackbar: SnackbarHostState,
): Boolean {
    val action by rememberUpdatedState(onAction)
    val latest by rememberUpdatedState(state)
    val language = LocalCoffeePeekLanguageTag.current
    val analytics = LocalShopAnalytics.current
    val content = shopDetailIosState(state, distanceLabel, formatCheckInDate, formatMenuDate,
        snackbar.currentSnackbarData?.visuals?.message, language)
    if (state.priceInfoVisible) analytics.Overlay {}
    checkNotNull(presenter) { "Native ShopDetail must be configured before opening iOS routes" }(
        ShopDetailIosPresentation(content, { name, id, index ->
            val intent = shopDetailIosIntent(name, id, index, latest)
            if (intent != null) {
                if (name == "hero-photo") analytics.content(ShopContentMetric.Photos, ShopContentAction.Open)
                if (name == "menu-photo") analytics.content(ShopContentMetric.Menu, ShopContentAction.Open)
                action(intent)
            }
        }, { snackbar.currentSnackbarData?.dismiss() }, { analytics.action(ShopActionMetric.Edit) }, language))
    return true
}
