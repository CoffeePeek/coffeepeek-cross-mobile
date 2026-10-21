package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CoffeePeekRatingSize(val starSize: Dp) { Small(16.dp), Medium(20.dp), Large(24.dp) }
enum class CoffeePeekRatingDisplay { Stars, Compact }

/**
 * Shared rating glyphs, not a business average. Read-only stars fill to the nearest tenth;
 * Compact is a rating symbol + value, never a one-star scale. Editable Stars emit 1..5:
 * the caller owns validation, state and optional tap-again-to-clear behavior.
 * [starDescription] lets feature-specific labels retain their localized contracts.
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
    starSpacing: Dp = if (onRatingChange == null) 3.dp else 1.dp,
    valueSpacing: Dp = 8.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
    valueColor: Color = LocalContentColor.current,
    valueStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    starDescription: @Composable (Int) -> String = { stringResource(Res.string.coffeepeek_rating_choose, it) },
) {
    require(onRatingChange == null || display == CoffeePeekRatingDisplay.Stars) { "Only Stars can be edited" }
    val value = coffeePeekRatingValue(rating)
    val description = contentDescription ?: stringResource(Res.string.coffeepeek_rating_description, coffeePeekRatingText(value))
    val behavior = if (onRatingChange == null) Modifier.semantics(mergeDescendants = true) {
        this.contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..5f)
    } else Modifier.widthIn(max = 224.dp).fillMaxWidth()
    Row(modifier.then(behavior), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(valueSpacing)) {
        if (display == CoffeePeekRatingDisplay.Compact) {
            Icon(CoffeePeekIcons.StarFilled, null, Modifier.size(size.starSize), tint = activeColor)
        } else Row(if (onRatingChange == null) Modifier else Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(starSpacing), verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                if (onRatingChange == null) {
                    CoffeePeekRatingStar(coffeePeekRatingStarFill(value, index), size.starSize, activeColor, inactiveColor)
                } else {
                    val label = starDescription(index + 1)
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp))
                        .selectable(selected = value == (index + 1).toDouble(), enabled = enabled, role = Role.Button) {
                            onRatingChange(index + 1)
                        }.semantics { this.contentDescription = label }, contentAlignment = Alignment.Center) {
                        CoffeePeekRatingStar(coffeePeekRatingStarFill(value, index), size.starSize, activeColor, inactiveColor)
                    }
                }
            }
        }
        if (valueText != null) Text(valueText, style = valueStyle, color = valueColor)
    }
}

@Composable
private fun CoffeePeekRatingStar(fill: Float, starSize: Dp, active: Color, inactive: Color) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.size(starSize)) {
        Icon(CoffeePeekIcons.StarOutline, null, Modifier.fillMaxSize(), tint = inactive)
        if (fill > 0f) Icon(CoffeePeekIcons.StarFilled, null,
            Modifier.fillMaxSize().drawWithContent {
                val boundary = this.size.width * fill
                clipRect(left = if (rtl) this.size.width - boundary else 0f,
                    right = if (rtl) this.size.width else boundary) { this@drawWithContent.drawContent() }
            }, tint = active)
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
            CoffeePeekRating(0.0, size = CoffeePeekRatingSize.Large, onRatingChange = {}, enabled = false)
        }
    }
}
@Preview @Composable private fun CoffeePeekRatingLightPreview() = CoffeePeekRatingPreview(false)
@Preview @Composable private fun CoffeePeekRatingDarkPreview() = CoffeePeekRatingPreview(true)
