package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** The application can render the existing picker actions with a native controller. */
internal val LocalCheckInCreatePicker = staticCompositionLocalOf<(@Composable (CheckInCreatePickerActions) -> Unit)?> { null }

internal class CheckInCreatePickerActions(
    val onDismiss: () -> Unit,
    val onShop: () -> Unit,
    val onHome: () -> Unit,
    val onShelfBag: () -> Unit,
)

internal fun GlobalCheckInFlow.pickerActions() = CheckInCreatePickerActions(
    onDismiss = { cancel(GlobalCheckInDestination.Picker) },
    onShop = ::shop,
    onHome = ::home,
    onShelfBag = ::shelfBag,
)
