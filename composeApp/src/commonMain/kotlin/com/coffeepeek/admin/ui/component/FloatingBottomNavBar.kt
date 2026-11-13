package com.coffeepeek.admin.ui.component

import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import com.coffeepeek.core.designsystem.component.CoffeePeekLargeContentViewer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.NavMetrics
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

val LocalFloatingNavClearance = compositionLocalOf { 0.dp }

@Composable
fun ProvideFloatingNavClearance(clearance: Dp, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFloatingNavClearance provides clearance, content = content)
}

data class FloatingNavItem(
    val title: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val isAction: Boolean = false,
    val modifier: Modifier = Modifier,
)

/** Compact glass navigation on Android; iOS uses its native UITabBarController. */
@Composable
fun FloatingBottomNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
) {
    if (items.isEmpty()) return
    val systemBars = WindowInsets.navigationBars
    val bottomGap = NavMetrics.floatingNavBottomGap(systemBars.asPaddingValues().calculateBottomPadding())
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .windowInsetsPadding(systemBars.only(WindowInsetsSides.Horizontal))
            .padding(bottom = bottomGap)
            .consumeWindowInsets(systemBars),
    ) {
        val availableSideSpace = (maxWidth - CoffeePeekDimens.glassControlSize * items.size - 20.dp) / 2
        val sideInset = availableSideSpace.coerceIn(0.dp, 21.dp)
        FloatingNavPill(items, Modifier.fillMaxWidth().padding(horizontal = sideInset), hazeState)
    }
}

@Composable
private fun FloatingNavPill(items: List<FloatingNavItem>, modifier: Modifier, hazeState: HazeState?) {
    BoxWithConstraints(modifier.height(NavMetrics.floatingNavBarHeight)
        .coffeePeekLiquidGlass(RoundedCornerShape(50), hazeState, shadowElevation = 10.dp).padding(4.dp)) {
        val selectedIndex = items.indexOfFirst { it.selected }
        val pitch = (maxWidth - 12.dp) / items.size
        if (selectedIndex >= 0) Box(Modifier.offset(x = pitch * selectedIndex).width(pitch + 12.dp)
            .fillMaxHeight().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(50)))
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp).selectableGroup()) {
            items.forEach { item -> FloatingNavBarItem(item, Modifier.weight(1f).fillMaxHeight()) }
        }
    }
}

@Composable
private fun FloatingNavBarItem(item: FloatingNavItem, modifier: Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val foreground by animateColorAsState(if (item.selected) colors.onSurface else colors.onSurfaceVariant,
        tween(160), label = "glass-tab-foreground")
    val highlight by animateColorAsState(if (pressed) colors.onSurface.copy(alpha = 0.12f) else Color.Transparent,
        tween(160), label = "glass-tab-press")
    Box(modifier.then(item.modifier)) {
        CoffeePeekLargeContentViewer(item.title, item.icon, Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().fillMaxHeight()
                .background(highlight, RoundedCornerShape(50))
                .then(if (item.isAction) Modifier.clickable(interactions, indication = null,
                    role = Role.Button, onClick = item.onClick)
                    else Modifier.selectable(item.selected, interactions, indication = null,
                        role = Role.Tab, onClick = item.onClick))
                .padding(horizontal = if (item.selected) 0.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (item.selected) 1.dp else 0.5.dp, Alignment.CenterVertically)) {
                FloatingNavBarCaption(item, foreground)
            }
        }
    }
}

@Composable
private fun FloatingNavBarCaption(item: FloatingNavItem, foreground: Color) {
    val tint = if (item.isAction || item.selected) MaterialTheme.colorScheme.coffeePeekAccentForeground else foreground
    Box(Modifier.height(if (item.selected) 21.dp else 28.dp), contentAlignment = Alignment.Center) {
        Icon(item.icon, null, Modifier.size(if (item.selected) 18.dp else 22.dp), tint = tint)
    }
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        val style = MaterialTheme.typography.labelSmall
        Text(item.title, Modifier.heightIn(min = 12.dp), color = foreground, style = style,
            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = 9.sp,
                maxFontSize = style.fontSize, stepSize = 0.5.sp))
    }
}

@Composable
private fun FloatingBottomNavBarPreview(dark: Boolean, scale: Float = 1f, width: Dp = 402.dp) =
    CoffeePeekTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
            FloatingBottomNavBar(listOf(
                FloatingNavItem("Поиск", CoffeePeekIcons.Search, true, {}),
                FloatingNavItem("Лента", CoffeePeekIcons.Community, false, {}),
                FloatingNavItem("Добавить", CoffeePeekIcons.Add, false, {}, isAction = true),
                FloatingNavItem("Профиль", CoffeePeekIcons.Profile, false, {}),
                FloatingNavItem("Настройки", CoffeePeekIcons.Settings, false, {}),
            ), Modifier.width(width))
        }
    }

@Preview @Composable private fun FloatingBottomNavBarLightPreview() = FloatingBottomNavBarPreview(false)
@Preview @Composable private fun FloatingBottomNavBarDarkPreview() = FloatingBottomNavBarPreview(true)

@Preview @Composable private fun FloatingBottomNavBarLargeLightPreview() = FloatingBottomNavBarPreview(false, 2f, 320.dp)
@Preview @Composable private fun FloatingBottomNavBarLargeDarkPreview() = FloatingBottomNavBarPreview(true, 2f, 320.dp)
