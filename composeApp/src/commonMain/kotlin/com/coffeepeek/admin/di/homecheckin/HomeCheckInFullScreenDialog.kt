package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.component.CoffeePeekFullScreenDialog

/** Application-owned full-screen editor surface; the choice picker remains a sheet. */
@Composable
internal fun HomeCheckInFullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    CoffeePeekFullScreenDialog(onDismiss, darkSystemBarIcons = background.luminance() > 0.5f) {
        Box(Modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            content()
        }
    }
}
