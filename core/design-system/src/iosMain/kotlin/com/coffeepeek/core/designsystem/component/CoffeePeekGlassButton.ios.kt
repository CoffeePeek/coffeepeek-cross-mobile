@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import dev.chrisbanes.haze.HazeState
import platform.UIKit.UIAction
import platform.UIKit.UIBackgroundConfiguration
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityValue

@Composable
actual fun CoffeePeekGlassButton(
    text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    isLoading: Boolean, loadingDescription: String?, leadingIcon: CoffeePeekGlassButtonIcon?, hazeState: HazeState?,
) {
    val currentOnClick = rememberUpdatedState(onClick)
    val currentEnabled = rememberUpdatedState(enabled && !isLoading)
    val loadingLabel = if (isLoading) requireNotNull(loadingDescription) else null
    val colors = MaterialTheme.colorScheme
    UIKitView(
        factory = {
            UIButton.buttonWithConfiguration(glassButtonConfiguration(), UIAction.actionWithHandler {
                if (currentEnabled.value) currentOnClick.value()
            })
        },
        modifier = modifier.height(CoffeePeekDimens.buttonHeight),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
        update = { button ->
            button.overrideUserInterfaceStyle = if (colors.background.luminance() < .5f)
                UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
            button.backgroundColor = UIColor.clearColor
            button.configuration = glassButtonConfiguration().apply {
                title = text
                image = when (leadingIcon) {
                    CoffeePeekGlassButtonIcon.Check -> UIImage.systemImageNamed("checkmark")
                    null -> null
                }
                imagePadding = 4.0
                showsActivityIndicator = isLoading
                cornerStyle = UIButtonConfigurationCornerStyleCapsule
                baseForegroundColor = colors.onSurface.toUIColor()
            }
            button.accessibilityLabel = text
            button.accessibilityValue = loadingLabel
            button.enabled = currentEnabled.value
        },
    )
}

private fun glassButtonConfiguration(): UIButtonConfiguration {
    if ((UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull() ?: 0) >= 26) {
        return UIButtonConfiguration.glassButtonConfiguration()
    }
    return UIButtonConfiguration.plainButtonConfiguration().apply {
        background = UIBackgroundConfiguration.clearConfiguration().apply {
            backgroundColor = UIColor.clearColor
            visualEffect = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular)
        }
    }
}

private fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble(),
)
