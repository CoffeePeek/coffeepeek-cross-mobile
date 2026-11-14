@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import dev.chrisbanes.haze.HazeState
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityValue

@Composable
actual fun CoffeePeekGlassButton(
    text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    isLoading: Boolean, loadingDescription: String?, leadingIcon: CoffeePeekGlassButtonIcon?, hazeState: HazeState?,
    glassTint: Color, contentColor: Color,
) {
    val currentOnClick = rememberUpdatedState(onClick)
    val currentEnabled = rememberUpdatedState(enabled && !isLoading)
    val loadingLabel = if (isLoading) requireNotNull(loadingDescription) else null
    val colors = MaterialTheme.colorScheme
    val foreground = contentColor.takeOrElse { colors.onSurface }
    val loaderParent = remember { mutableStateOf<UIView?>(null) }
    CoffeePeekNativeGlassView(
        factory = {
            UIButton.buttonWithConfiguration(coffeePeekGlassButtonConfiguration(), UIAction.actionWithHandler {
                if (currentEnabled.value) currentOnClick.value()
            })
        },
        modifier = modifier.sizeIn(minWidth = CoffeePeekDimens.minimumTouchTarget, minHeight = CoffeePeekDimens.buttonHeight),
        update = { button ->
            button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(glassTint.takeOrElse { colors.background })
            button.backgroundColor = UIColor.clearColor
            button.configuration = coffeePeekGlassButtonConfiguration(
                prominent = glassTint.isSpecified && coffeePeekLiquidGlassAvailable,
            ).apply {
                title = text
                image = if (isLoading) coffeePeekLoaderPlaceholderImage() else when (leadingIcon) {
                    CoffeePeekGlassButtonIcon.Check -> UIImage.systemImageNamed("checkmark")
                    null -> null
                }
                imagePadding = 4.0
                showsActivityIndicator = false
                cornerStyle = UIButtonConfigurationCornerStyleCapsule
                if (glassTint.isSpecified && coffeePeekLiquidGlassAvailable) baseBackgroundColor = glassTint.toUIColor()
                baseForegroundColor = foreground.toUIColor()
            }
            button.titleLabel?.adjustsFontForContentSizeCategory = true
            button.titleLabel?.numberOfLines = 0
            button.accessibilityLabel = text
            button.accessibilityValue = loadingLabel
            button.enabled = currentEnabled.value
            loaderParent.value = if (isLoading) button.imageView else null
        },
    )
    CoffeePeekNativeLoader(loaderParent.value, foreground)
}

internal fun coffeePeekUserInterfaceStyle(background: Color): UIUserInterfaceStyle =
    if (background.luminance() < .5f) UIUserInterfaceStyle.UIUserInterfaceStyleDark
    else UIUserInterfaceStyle.UIUserInterfaceStyleLight

internal fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble(),
)
