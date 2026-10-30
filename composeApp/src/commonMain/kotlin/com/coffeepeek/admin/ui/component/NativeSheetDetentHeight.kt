package com.coffeepeek.admin.ui.component

import com.coffeepeek.core.designsystem.component.coffeePeekNativeSheetDetentHeight

internal fun nativeSheetDetentHeight(contentHeight: Double, maximumHeight: Double, heightFraction: Float): Double =
    coffeePeekNativeSheetDetentHeight(contentHeight, maximumHeight, heightFraction)
