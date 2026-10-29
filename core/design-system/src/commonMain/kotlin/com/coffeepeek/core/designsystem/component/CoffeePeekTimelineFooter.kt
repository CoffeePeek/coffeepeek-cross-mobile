package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Display-only affinity and saved drink, without a fake click target or a ripple. */
@Composable
fun CoffeePeekTimelineFooter(count: Int?, selected: Boolean, likesDescription: String, drink: String?, modifier: Modifier = Modifier) {
    if (count == null && drink == null) return
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (count != null) Row(Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = likesDescription },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            CoffeePeekLikeIcon(selected, null)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.weight(1f))
        drink?.let { CoffeePeekDrinkLabel(it, size = CoffeePeekDrinkLabelSize.Compact) }
    }
}

@Composable private fun TimelineFooterPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekTimelineFooter(3, true, "Лайки: 3", "Капучино")
}
@Preview @Composable private fun TimelineFooterLightPreview() = TimelineFooterPreview(false)
@Preview @Composable private fun TimelineFooterDarkPreview() = TimelineFooterPreview(true)
