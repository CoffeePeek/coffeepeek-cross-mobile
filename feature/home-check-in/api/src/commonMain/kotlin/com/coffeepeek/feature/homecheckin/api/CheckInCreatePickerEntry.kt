package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Direct-action type picker; the app owns the sheet, account gate and destination callbacks.
 * [onShelfBag] adds the third choice, adding a bag to the shelf, where the shelf is available.
 */
interface CheckInCreatePickerEntry {
    @Composable
    fun Content(onShop: () -> Unit, onHome: () -> Unit, modifier: Modifier, onShelfBag: (() -> Unit)? = null)
}
