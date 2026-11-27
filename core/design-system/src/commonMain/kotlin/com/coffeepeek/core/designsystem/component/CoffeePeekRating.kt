package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_rating_description
import com.coffeepeek.core.designsystem.resources.coffeepeek_rating_choose
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import kotlin.math.roundToInt
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CoffeePeekRatingSize(val starSize: Dp) { Small(16.dp), Medium(20.dp), Large(24.dp) }
enum class CoffeePeekRatingDisplay { Stars, Compact }

/**
 * Shared rating glyphs, not a business average. Read-only stars fill to the nearest tenth;
 * Compact is a rating symbol + value, never a one-star scale. Editable Stars emit 1..5:
 * the caller owns validation, state and optional tap-again-to-clear behavior.
 * [starDescription] lets feature-specific labels retain their localized contracts.
 * [spreadChoices] gives editable stars equal space across a bounded form row.
 */
@Composable
fun CoffeePeekRating(
    rating: Double,
    modifier: Modifier = Modifier,
    size: CoffeePeekRatingSize = CoffeePeekRatingSize.Medium,
    display: CoffeePeekRatingDisplay = CoffeePeekRatingDisplay.Stars,
    valueText: String? = if (display == CoffeePeekRatingDisplay.Compact) coffeePeekRatingText(rating) else null,
    contentDescription: String? = null,
    onRatingChange: ((Int) -> Unit)? = null,
    enabled: Boolean = true,
    starSpacing: Dp = 4.dp,
    valueSpacing: Dp = 8.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = LocalContentColor.current,
    valueStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    spreadChoices: Boolean = false,
    starDescription: @Composable (Int) -> String = { stringResource(Res.string.coffeepeek_rating_choose, it) },
) {
    require(onRatingChange == null || display == CoffeePeekRatingDisplay.Stars) { "Only Stars can be edited" }
    val value = coffeePeekRatingValue(rating)
    val description = contentDescription ?: stringResource(Res.string.coffeepeek_rating_description, coffeePeekRatingText(value))
    val spread = spreadChoices && onRatingChange != null
    // Compact input scrolls in narrow rows. Spread input divides the available width into five targets.
    val behavior = if (onRatingChange == null) Modifier.semantics(mergeDescendants = true) {
        this.contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..5f)
    } else if (spread) Modifier.fillMaxWidth() else Modifier.widthIn(max = coffeePeekRatingInputWidth(size.starSize, starSpacing))
    Row(modifier.then(behavior), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(valueSpacing)) {
        // Reserve an editable caption before measuring the scrolling stars, without stretching them.
        val starsModifier = if (spread || (onRatingChange != null && valueText != null)) Modifier.weight(1f, fill = spread) else Modifier
        if (display == CoffeePeekRatingDisplay.Compact) {
            CoffeePeekRatingStar(1f, size.starSize, activeColor, inactiveColor)
        } else CoffeePeekRatingStars(value, size.starSize, enabled, starSpacing, activeColor, inactiveColor,
            onRatingChange, starDescription, starsModifier, spread)
        if (valueText != null) Text(valueText, style = valueStyle, color = valueColor)
    }
}

internal fun coffeePeekRatingInputWidth(starSize: Dp, spacing: Dp): Dp = starSize * 5 + spacing * 4

@Composable
private fun CoffeePeekRatingStars(value: Double, starSize: Dp, enabled: Boolean, spacing: Dp,
    active: Color, inactive: Color, onChange: ((Int) -> Unit)?, description: @Composable (Int) -> String,
    modifier: Modifier, spread: Boolean) {
    val scrolling = if (onChange == null || spread) Modifier else Modifier.horizontalScroll(rememberScrollState())
    Row(modifier.then(scrolling), horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            if (onChange == null) CoffeePeekRatingStar(coffeePeekRatingStarFill(value, index), starSize, active, inactive)
            else {
                val target = if (spread) Modifier.weight(1f).height(CoffeePeekDimens.minimumTouchTarget) else Modifier.size(starSize)
                CoffeePeekRatingChoice(index + 1, value, starSize, enabled, active, inactive, description(index + 1), onChange, target)
            }
        }
    }
}

@Composable
private fun CoffeePeekRatingChoice(score: Int, value: Double, starSize: Dp, enabled: Boolean,
    active: Color, inactive: Color, description: String, onChange: (Int) -> Unit, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp))
        .selectable(selected = value == score.toDouble(), enabled = enabled, role = Role.Button) { onChange(score) }
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        CoffeePeekRatingStar(coffeePeekRatingStarFill(value, score - 1), starSize, active, inactive)
    }
}

@Composable
private fun CoffeePeekRatingStar(fill: Float, starSize: Dp, active: Color, inactive: Color) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = MaterialTheme.colorScheme
    val light = colors.surface.luminance() >= 0.5f
    Box(Modifier.size(starSize)) {
        if (fill > 0f) Box(Modifier.matchParentSize().clipRatingFill(fill, rtl)) {
            CoffeePeekIcon(CoffeePeekIcons.StarFilled, null, Modifier.matchParentSize(), tint = active)
            CoffeePeekIcon(CoffeePeekIcons.StarOutline, null, Modifier.matchParentSize(), tint = active)
        }
        if (fill < 1f) {
            CoffeePeekIcon(CoffeePeekIcons.StarOutline, null, Modifier.fillMaxSize().clipRatingFill(1f - fill, !rtl),
                tint = if (light) colors.onSurface else inactive)
        }
    }
}

private fun Modifier.clipRatingFill(fill: Float, rtl: Boolean) = drawWithContent {
    val boundary = size.width * fill
    clipRect(left = if (rtl) size.width - boundary else 0f, right = if (rtl) size.width else boundary) {
        this@drawWithContent.drawContent()
    }
}

internal fun coffeePeekRatingValue(rating: Double): Double =
    if (rating.isFinite()) (rating.coerceIn(0.0, 5.0) * 10).roundToInt() / 10.0 else 0.0

internal fun coffeePeekRatingStarFill(rating: Double, index: Int): Float =
    (coffeePeekRatingValue(rating) - index).coerceIn(0.0, 1.0).toFloat()

internal fun coffeePeekRatingText(rating: Double): String {
    val tenths = (coffeePeekRatingValue(rating) * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Composable private fun CoffeePeekRatingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CoffeePeekRating(4.3, size = CoffeePeekRatingSize.Small, valueText = "4.3")
            CoffeePeekRating(3.7, valueText = "3.7")
            CoffeePeekRating(4.5, size = CoffeePeekRatingSize.Large, valueText = "4.5")
            CoffeePeekRating(4.3, display = CoffeePeekRatingDisplay.Compact)
            CoffeePeekRating(4.0, size = CoffeePeekRatingSize.Large, onRatingChange = {})
            CoffeePeekRating(4.0, size = CoffeePeekRatingSize.Large, onRatingChange = {}, spreadChoices = true)
            CoffeePeekRating(0.0, size = CoffeePeekRatingSize.Large, onRatingChange = {}, enabled = false)
            CoffeePeekRating(3.0, modifier = Modifier.width(96.dp), onRatingChange = {})
            CoffeePeekRating(4.0, size = CoffeePeekRatingSize.Large, valueText = "4/5", onRatingChange = {})
            CoffeePeekRating(4.0, modifier = Modifier.width(96.dp), size = CoffeePeekRatingSize.Large,
                valueText = "4/5", onRatingChange = {})
        }
    }
}
@Preview @Composable private fun CoffeePeekRatingLightPreview() = CoffeePeekRatingPreview(false)
@Preview @Composable private fun CoffeePeekRatingDarkPreview() = CoffeePeekRatingPreview(true)
