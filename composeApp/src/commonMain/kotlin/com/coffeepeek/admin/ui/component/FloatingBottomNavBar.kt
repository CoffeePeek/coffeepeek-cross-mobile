package com.coffeepeek.admin.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.admin.ui.icons.CpIcons
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
)

/** Compact glass navigation on Android; iOS uses its native UITabBarController. */
@Composable
fun FloatingBottomNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalGlassHazeState.current,
) {
    val height = floatingNavBarHeight()
    Row(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 16.dp)
            .padding(bottom = FloatingNavBottomMargin).fillMaxWidth()
            .height(height)
            .liquidGlass(RoundedCornerShape(32.dp), hazeState, shadowElevation = 10.dp)
            .padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item -> FloatingNavBarItem(item) }
    }
}

// HIG-style compact tab captions; larger content is available on long press.
internal fun floatingNavBarHeight(): Dp = 64.dp

val FloatingNavBottomMargin = 8.dp

@Composable
private fun RowScope.FloatingNavBarItem(item: FloatingNavItem) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val foreground by animateColorAsState(if (item.selected) colors.onSurface else colors.onSurfaceVariant,
        tween(160), label = "glass-tab-foreground")
    val highlight by animateColorAsState(when {
        pressed -> colors.onSurface.copy(alpha = 0.12f)
        item.selected -> colors.onSurface.copy(alpha = 0.07f)
        else -> Color.Transparent
    }, tween(160), label = "glass-tab-highlight")
    Box(Modifier.weight(1f).fillMaxHeight()) {
        CoffeePeekLargeContentViewer(item.title, item.icon, Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().fillMaxHeight()
                .background(highlight, RoundedCornerShape(24.dp))
                .selectable(item.selected, interactions, indication = null, role = Role.Tab, onClick = item.onClick)
                .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                Icon(item.icon, null, Modifier.size(22.dp), tint = if (item.selected) colors.coffeePeekAccentForeground else foreground)
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                    Text(item.title, color = foreground, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = 9.sp,
                            maxFontSize = MaterialTheme.typography.labelSmall.fontSize, stepSize = 0.5.sp))
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomNavBarPreview(dark: Boolean, scale: Float = 1f) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(LocalDensity.current.density, scale)) {
        FloatingBottomNavBar(listOf(
            FloatingNavItem("Поиск", CpIcons.Search, true, {}),
            FloatingNavItem("Кофе", CpIcons.CoffeeBean, false, {}),
            FloatingNavItem("Лента", CpIcons.Community, false, {}),
            FloatingNavItem("Профиль", CpIcons.Profile, false, {}),
            FloatingNavItem("Настройки", CpIcons.Settings, false, {}),
        ))
    }
}

@Preview @Composable private fun FloatingBottomNavBarLightPreview() = FloatingBottomNavBarPreview(false)
@Preview @Composable private fun FloatingBottomNavBarDarkPreview() = FloatingBottomNavBarPreview(true)

@Preview @Composable private fun FloatingBottomNavBarLargeLightPreview() = FloatingBottomNavBarPreview(false, 2f)
@Preview @Composable private fun FloatingBottomNavBarLargeDarkPreview() = FloatingBottomNavBarPreview(true, 2f)
