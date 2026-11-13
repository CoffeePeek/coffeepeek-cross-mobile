package com.coffeepeek.admin.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Space the floating navigation takes at the bottom of a screen. */
internal object NavMetrics {
    val floatingNavBarHeight = 62.dp
    val floatingNavContentClearance = floatingNavBarHeight + 8.dp
    val nativeOperationClearance = 64.dp
    val iosTabBarClearanceFloor = 83.dp

    fun floatingNavBottomGap(systemBottomInset: Dp): Dp = maxOf(21.dp, systemBottomInset + 8.dp)

    fun floatingNavClearance(systemBottomInset: Dp): Dp =
        floatingNavBarHeight + floatingNavBottomGap(systemBottomInset)
}
