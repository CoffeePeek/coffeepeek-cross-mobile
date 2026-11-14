package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
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
        // The editor consumes IME space; this host reserves only the screen's safe edges.
        val screenInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        Box(Modifier.fillMaxSize().background(background).windowInsetsPadding(screenInsets)) {
            content()
        }
    }
}
