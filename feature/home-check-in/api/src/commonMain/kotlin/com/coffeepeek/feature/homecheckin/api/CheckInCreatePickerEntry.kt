package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Type-picker entry with local selection; the app owns the sheet, account gate and destination callbacks. */
interface CheckInCreatePickerEntry {
    @Composable
    fun Content(onShop: () -> Unit, onHome: () -> Unit, modifier: Modifier)
}
