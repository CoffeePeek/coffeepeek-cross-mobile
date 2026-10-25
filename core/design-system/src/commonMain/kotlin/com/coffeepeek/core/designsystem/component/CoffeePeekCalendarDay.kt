package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.tooling.preview.Preview

/** Caller owns dates, interaction and accessibility. Photos replace the centered date entirely. */
@Composable
fun CoffeePeekCalendarDay(day: Int, isToday: Boolean, isFuture: Boolean,
    modifier: Modifier = Modifier, isSelected: Boolean = false,
    photo: (@Composable (Modifier) -> Unit)? = null, footer: (@Composable () -> Unit)? = null) {
    val style = coffeePeekCalendarDayStyle(isToday, isFuture, photo != null, isSelected)
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.clip(shape)
        .background(if (style.selectedFill) MaterialTheme.colorScheme.primary else Color.Transparent)
        .then(if (style.outlined) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)) {
        Column(Modifier.fillMaxSize().padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (style.showDate) Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (style.emphasized) FontWeight.SemiBold else FontWeight.Normal,
                    color = calendarDayColor(style.tone), modifier = Modifier.clearAndSetSemantics {})
                else photo?.invoke(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).clearAndSetSemantics {})
            }
            footer?.invoke()
        }
    }
}

@Composable
private fun calendarDayColor(tone: CoffeePeekCalendarDayTone): Color = when (tone) {
    CoffeePeekCalendarDayTone.Accent -> MaterialTheme.colorScheme.primary
    CoffeePeekCalendarDayTone.OnAccent -> MaterialTheme.colorScheme.onPrimary
    CoffeePeekCalendarDayTone.Muted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    CoffeePeekCalendarDayTone.Content -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun CalendarDayPreview() {
    Row {
        CoffeePeekCalendarDay(23, false, false, Modifier.size(48.dp))
        CoffeePeekCalendarDay(24, true, false, Modifier.size(48.dp))
        CoffeePeekCalendarDay(25, false, true, Modifier.size(48.dp))
        CoffeePeekCalendarDay(24, true, false, Modifier.size(48.dp), photo = {
            Box(it.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer))
        })
    }
}

@Preview @Composable private fun CoffeePeekCalendarDayLightPreview() = CoffeePeekTheme(darkTheme = false) { CalendarDayPreview() }
@Preview @Composable private fun CoffeePeekCalendarDayDarkPreview() = CoffeePeekTheme(darkTheme = true) { CalendarDayPreview() }
