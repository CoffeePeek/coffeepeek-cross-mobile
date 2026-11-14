package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
internal actual fun Modifier.closeKeyboardOnDrag(): Modifier = this.imeNestedScroll()
