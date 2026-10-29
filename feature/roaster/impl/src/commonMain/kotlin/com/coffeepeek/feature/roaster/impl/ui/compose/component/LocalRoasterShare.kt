package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
internal val LocalRoasterShare = staticCompositionLocalOf<(String) -> Unit> { {} }
