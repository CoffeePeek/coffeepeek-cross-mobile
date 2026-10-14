@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import dev.chrisbanes.haze.HazeState
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.accessibilityLabel
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarDelegateProtocol
import platform.UIKit.UITabBarItem
import platform.darwin.NSObject

private val isLiquidGlassAvailable: Boolean
    get() = (UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull() ?: 0) >= 26

private val GlassControlIcon.symbol: String
    get() = when (this) {
        GlassControlIcon.Back -> "chevron.left"
        GlassControlIcon.Add -> "plus"
        GlassControlIcon.Edit -> "square.and.pencil"
        GlassControlIcon.Favorite -> "heart"
        GlassControlIcon.FavoriteFilled -> "heart.fill"
        GlassControlIcon.Share -> "square.and.arrow.up"
        GlassControlIcon.Close -> "xmark"
        GlassControlIcon.Zones -> "square.3.layers.3d"
        GlassControlIcon.Location -> "location.north.fill"
    }

@Composable
actual fun PlatformGlassIconButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier,
    enabled: Boolean,
    selected: Boolean,
    isLoading: Boolean,
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    val currentOnClick = rememberUpdatedState(onClick)
    val currentIcon = rememberUpdatedState(icon)
    val colors = MaterialTheme.colorScheme
    UIKitView(
        factory = {
            UIButton.buttonWithConfiguration(
                configuration = UIButtonConfiguration.grayButtonConfiguration(),
                primaryAction = UIAction.actionWithHandler {
                    if (currentIcon.value == GlassControlIcon.Favorite || currentIcon.value == GlassControlIcon.FavoriteFilled) platformHaptic(HapticEvent.Selection)
                    currentOnClick.value()
                },
            )
        },
        modifier = modifier.size(CpDimens.buttonHeight),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
        update = { button ->
            button.overrideUserInterfaceStyle = colors.uiKitStyle
            button.backgroundColor = UIColor.clearColor
            val configuration = if (isLiquidGlassAvailable) {
                UIButtonConfiguration.glassButtonConfiguration()
            } else {
                UIButtonConfiguration.grayButtonConfiguration()
            }
            configuration.cornerStyle = UIButtonConfigurationCornerStyleCapsule
            configuration.image = UIImage.systemImageNamed(icon.symbol)
            configuration.showsActivityIndicator = isLoading
            configuration.baseForegroundColor = colors.onSurface.toUIColor()
            if (selected) {
                configuration.baseForegroundColor = colors.primary.toUIColor()
            } else if (icon == GlassControlIcon.FavoriteFilled) {
                configuration.baseForegroundColor = UIColor.colorWithRed(
                    239.0 / 255.0, 68.0 / 255.0, 68.0 / 255.0, 1.0,
                )
            }
            button.configuration = configuration
            button.accessibilityLabel = contentDescription
            button.enabled = enabled && !isLoading
        },
    )
}

@Composable
actual fun PlatformGlassBackButton(onClick: () -> Unit, modifier: Modifier) {
    val currentOnClick = rememberUpdatedState(onClick)
    val colors = MaterialTheme.colorScheme
    UIKitView(
        factory = {
            UIButton.buttonWithConfiguration(
                configuration = UIButtonConfiguration.grayButtonConfiguration(),
                primaryAction = UIAction.actionWithHandler { currentOnClick.value() },
            )
        },
        modifier = modifier.height(CpDimens.buttonHeight).width(104.dp),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
        update = { button ->
            button.overrideUserInterfaceStyle = colors.uiKitStyle
            button.backgroundColor = UIColor.clearColor
            val configuration = if (isLiquidGlassAvailable) {
                UIButtonConfiguration.glassButtonConfiguration()
            } else {
                UIButtonConfiguration.grayButtonConfiguration()
            }
            configuration.cornerStyle = UIButtonConfigurationCornerStyleCapsule
            configuration.baseForegroundColor = colors.onSurface.toUIColor()
            configuration.image = UIImage.systemImageNamed(GlassControlIcon.Back.symbol)
            configuration.title = "Назад"
            button.configuration = configuration
            button.accessibilityLabel = "Назад"
        },
    )
}

@Composable
actual fun PlatformMapControlButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier,
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    PlatformGlassIconButton(
        icon = icon,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        selected = selected,
        hazeState = null,
        content = content,
    )
}

@Composable
actual fun PlatformFloatingBottomNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier,
    hazeState: HazeState?,
) {
    val delegate = remember { CoffeeTabBarDelegate() }
    val colors = MaterialTheme.colorScheme
    delegate.callbacks = items.map { it.onClick }
    val bottomInset = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    UIKitView(
        factory = {
            UITabBar().apply {
                this.delegate = delegate
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp + bottomInset),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
        update = { bar ->
            bar.overrideUserInterfaceStyle = colors.uiKitStyle
            bar.tintColor = colors.primary.toUIColor()
            bar.unselectedItemTintColor = colors.onSurfaceVariant.toUIColor()
            if (bar.items?.size != items.size) {
                bar.items = items.mapIndexed { index, item ->
                    UITabBarItem(
                        title = item.title,
                        image = UIImage.systemImageNamed(item.iconSymbol),
                        tag = index.toLong(),
                    )
                }
            }
            bar.selectedItem = bar.items?.getOrNull(items.indexOfFirst { it.selected }) as? UITabBarItem
        },
    )
}

private val FloatingNavItem.iconSymbol: String
    get() = when (title) {
        "Поиск" -> "magnifyingglass"
        "Кофе" -> "leaf"
        "Лента" -> "person.2"
        "Карта" -> "map.fill"
        "Профиль" -> "person.crop.circle.fill"
        "Настройки" -> "gearshape.fill"
        else -> "circle.fill"
    }

private class CoffeeTabBarDelegate : NSObject(), UITabBarDelegateProtocol {
    var callbacks: List<() -> Unit> = emptyList()

    override fun tabBar(tabBar: UITabBar, didSelectItem: UITabBarItem) {
        callbacks.getOrNull(didSelectItem.tag.toInt())?.invoke()
    }
}
