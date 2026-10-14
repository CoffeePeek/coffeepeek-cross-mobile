@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIColor

@Composable
internal actual fun PlatformActivityIndicator(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    UIKitView(
        factory = {
            UIActivityIndicatorView().apply {
                backgroundColor = UIColor.clearColor
                startAnimating()
            }
        },
        modifier = modifier.semantics {
            contentDescription = "Проверка обновлений"
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        properties = UIKitInteropProperties(interactionMode = null, placedAsOverlay = true),
        update = {
            it.color = colors.onSurfaceVariant.toUIColor()
            it.overrideUserInterfaceStyle = colors.uiKitStyle
        },
        onRelease = { it.stopAnimating() },
    )
}
