package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHostState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState

@Composable
internal expect fun PlatformShopDetailContent(
    state: ShopDetailState, onAction: (ShopDetailAction) -> Unit, distanceLabel: String?,
    formatCheckInDate: (String) -> String, formatMenuDate: (String) -> String, snackbar: SnackbarHostState,
): Boolean
