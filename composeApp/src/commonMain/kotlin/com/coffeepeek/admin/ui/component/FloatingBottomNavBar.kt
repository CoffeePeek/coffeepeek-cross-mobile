package com.coffeepeek.admin.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.coffeepeek.core.designsystem.component.coffeePeekIconPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.coffeepeek.admin.ui.NavMetrics
import com.coffeepeek.core.designsystem.component.CoffeePeekLargeContentViewer
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
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
        key(items.map { it.icon }) {
            val selectedIndex = items.indexOfFirst { it.selected && !it.isAction }
            val selection = updateTransition(selectedIndex, label = "navigation-selection")
            val pitch = (maxWidth - 12.dp).coerceAtLeast(0.dp) / items.size
            if (selectedIndex >= 0) FloatingNavSelection(selection, pitch, items.lastIndex)
            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp).selectableGroup()) {
                items.forEachIndexed { index, item ->
                    val progress = selection.animateFloat(transitionSpec = { selectionMotion() }, label = item.title) {
                        if (it == index) 1f else 0f
                    }
                    FloatingNavBarItem(item, progress, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

private fun Transition.Segment<Int>.selectionMotion() =
    if (initialState < 0 || targetState < 0) snap<Float>() else tween<Float>(220)

@Composable
private fun FloatingNavSelection(selection: Transition<Int>, pitch: Dp, lastIndex: Int) {
    val position = selection.animateFloat(transitionSpec = { selectionMotion() }, label = "selection-position") {
        it.coerceAtLeast(0).toFloat()
    }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Box(Modifier.width(pitch + 12.dp).fillMaxHeight().graphicsLayer {
        translationX = pitch.toPx() * position.value.coerceIn(0f, lastIndex.toFloat()) * direction
    }.background(background, RoundedCornerShape(50)))
}

@Composable
private fun FloatingNavBarItem(item: FloatingNavItem, selection: State<Float>, modifier: Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val highlight by animateColorAsState(if (pressed) colors.onSurface.copy(alpha = 0.12f) else Color.Transparent,
        tween(reducedMotionDuration(160)), label = "glass-tab-press")
    Box(modifier.then(item.modifier)) {
        CoffeePeekLargeContentViewer(item.title, item.icon, Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()
                .background(highlight, RoundedCornerShape(50))
                .then(if (item.isAction) Modifier.clickable(interactions, indication = null,
                    role = Role.Button, onClick = item.onClick)
                    else Modifier.selectable(item.selected, interactions, indication = null,
                        role = Role.Tab, onClick = item.onClick))
                .semantics { text = AnnotatedString(item.title) }) {
                FloatingNavBarCaption(item, selection)
            }
        }
    }
}

@Composable
private fun FloatingNavBarCaption(item: FloatingNavItem, selection: State<Float>) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.coffeePeekAccentForeground
    val icon = coffeePeekIconPainter(item.icon)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val (inactive, active) = rememberNavCaptions(item.title, constraints.maxWidth)
        Canvas(Modifier.fillMaxSize()) {
            val progress = selection.value.coerceIn(0f, 1f)
            val iconHeight = lerp(28.dp.toPx(), 21.dp.toPx(), progress)
            val iconSize = lerp(22.dp.toPx(), 18.dp.toPx(), progress)
            val gap = lerp(0.5.dp.toPx(), 1.dp.toPx(), progress)
            val captionHeight = lerp(inactive.size.height.toFloat(), active.size.height.toFloat(), progress)
            val contentTop = (size.height - iconHeight - gap - maxOf(12.dp.toPx(), captionHeight)) / 2f
            val tint = if (item.isAction) accent else lerp(colors.onSurfaceVariant, accent, progress)
            translate((size.width - iconSize) / 2f, contentTop + (iconHeight - iconSize) / 2f) {
                with(icon) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(tint)) }
            }
            val labelCenter = Offset(size.width / 2f, contentTop + iconHeight + gap + captionHeight / 2f)
            drawNavCaption(inactive, active, labelCenter, lerp(colors.onSurfaceVariant, colors.onSurface, progress), progress)
        }
    }
}

@Composable
private fun rememberNavCaptions(title: String, width: Int): Pair<TextLayoutResult, TextLayoutResult> {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current.density
    return remember(title, width, measurer, style, density) {
        val compactDensity = Density(density, fontScale = 1f)
        val inset = with(compactDensity) { 4.dp.roundToPx() * 2 }
        val inactive = measurer.measureNavCaption(title, style.copy(fontWeight = FontWeight.Medium),
            compactDensity, (width - inset).coerceAtLeast(0))
        val active = measurer.measureNavCaption(title, style.copy(fontWeight = FontWeight.SemiBold), compactDensity, width)
        inactive to active
    }
}

/** Fit the stable slot endpoints once. Animation scales the cached glyphs instead of fitting every frame. */
private fun TextMeasurer.measureNavCaption(title: String, style: TextStyle, density: Density, width: Int): TextLayoutResult {
    val text = AnnotatedString(title)
    val constraints = Constraints(maxWidth = width)
    var fontSize = style.fontSize.value
    while (fontSize > 9f) {
        val layout = measure(text, style.copy(fontSize = fontSize.sp), maxLines = 1,
            softWrap = false, constraints = constraints, density = density)
        if (!layout.hasVisualOverflow) return layout
        fontSize -= 0.5f
    }
    return measure(text, style.copy(fontSize = 9.sp), maxLines = 1, softWrap = false,
        overflow = TextOverflow.Ellipsis, constraints = constraints, density = density)
}

private fun DrawScope.drawNavCaption(
    inactive: TextLayoutResult, active: TextLayoutResult, center: Offset, color: Color, progress: Float,
) {
    val fontSize = lerp(inactive.layoutInput.style.fontSize.value, active.layoutInput.style.fontSize.value, progress)
    if (progress < 1f) drawScaledNavCaption(inactive, center, color, fontSize, 1f - progress)
    if (progress > 0f) drawScaledNavCaption(active, center, color, fontSize, progress)
}

private fun DrawScope.drawScaledNavCaption(
    layout: TextLayoutResult, center: Offset, color: Color, fontSize: Float, alpha: Float,
) {
    val textScale = fontSize / layout.layoutInput.style.fontSize.value
    val topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f)
    scale(textScale, textScale, pivot = center) { drawText(layout, color, topLeft, alpha = alpha) }
}

@Composable
private fun FloatingBottomNavBarPreview(dark: Boolean, scale: Float = 1f, width: Dp = 402.dp) =
    CoffeePeekTheme(darkTheme = dark) {
        var selected by remember { mutableIntStateOf(0) }
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
            FloatingBottomNavBar(listOf(
                FloatingNavItem("Поиск", CoffeePeekIcons.Search, selected == 0, { selected = 0 }),
                FloatingNavItem("Лента", CoffeePeekIcons.Community, selected == 1, { selected = 1 }),
                FloatingNavItem("Добавить", CoffeePeekIcons.Add, false, {}, isAction = true),
                FloatingNavItem("Профиль", CoffeePeekIcons.Profile, selected == 3, { selected = 3 }),
                FloatingNavItem("Настройки", CoffeePeekIcons.Settings, selected == 4, { selected = 4 }),
            ), Modifier.width(width))
        }
    }

@Preview @Composable private fun FloatingBottomNavBarLightPreview() = FloatingBottomNavBarPreview(false)
@Preview @Composable private fun FloatingBottomNavBarDarkPreview() = FloatingBottomNavBarPreview(true)

@Preview @Composable private fun FloatingBottomNavBarLargeLightPreview() = FloatingBottomNavBarPreview(false, 2f, 320.dp)
@Preview @Composable private fun FloatingBottomNavBarLargeDarkPreview() = FloatingBottomNavBarPreview(true, 2f, 320.dp)
