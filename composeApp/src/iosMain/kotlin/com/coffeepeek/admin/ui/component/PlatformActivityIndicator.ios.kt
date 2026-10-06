@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIColor

@Composable
internal actual fun PlatformActivityIndicator(modifier: Modifier) {
    UIKitView(
        factory = {
            UIActivityIndicatorView().apply {
                color = UIColor.secondaryLabelColor
                startAnimating()
            }
        },
        modifier = modifier.semantics {
            contentDescription = "Проверка обновлений"
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        properties = UIKitInteropProperties(interactionMode = null),
        onRelease = { it.stopAnimating() },
    )
}
