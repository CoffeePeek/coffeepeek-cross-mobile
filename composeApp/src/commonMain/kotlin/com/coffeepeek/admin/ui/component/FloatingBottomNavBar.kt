package com.coffeepeek.admin.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import dev.chrisbanes.haze.HazeState

/** Extra bottom space so list content / FABs clear the bottom navigation. */
val LocalFloatingNavClearance = compositionLocalOf { 0.dp }

@Composable
fun ProvideFloatingNavClearance(
    clearance: Dp,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFloatingNavClearance provides clearance) {
        content()
    }
}

data class FloatingNavItem(
    val title: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** Compose floating bar used on Android; iOS hosts a native tab bar. */
@Composable
fun FloatingBottomNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalGlassHazeState.current,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = CpDimens.spacing4)
            .padding(bottom = FloatingNavBottomMargin)
            .fillMaxWidth()
            .height(CpDimens.floatingNavBarHeight)
            .liquidGlass(shape, hazeState, shadowElevation = 10.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            FloatingNavBarItem(item = item)
        }
    }
}

/** Gap between the floating bar and the system navigation area. */
val FloatingNavBottomMargin = 8.dp

@Composable
private fun RowScope.FloatingNavBarItem(item: FloatingNavItem) {
    val contentColor by animateColorAsState(
        targetValue = if (item.selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(180),
        label = "floating-nav-content",
    )
    val pillColor by animateColorAsState(
        targetValue = if (item.selected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(220),
        label = "floating-nav-pill",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .background(pillColor)
            .selectable(
                selected = item.selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = item.onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
