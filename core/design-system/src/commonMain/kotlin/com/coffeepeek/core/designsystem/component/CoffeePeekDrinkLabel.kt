package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CoffeePeekDrinkLabelSize { Regular, Compact }

/** Read-only drink image and name: no plate, border, chevron or implied action.
 * The owner supplies an image renderer; this component never loads a URL itself.
 */
@Composable
fun CoffeePeekDrinkLabel(
    name: String,
    modifier: Modifier = Modifier,
    imageContent: (@Composable (Modifier) -> Unit)? = null,
    size: CoffeePeekDrinkLabelSize = CoffeePeekDrinkLabelSize.Regular,
) {
    val compact = size == CoffeePeekDrinkLabelSize.Compact
    val iconSize = if (compact) 14.dp else 32.dp
    Row(modifier.widthIn(max = 200.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) CoffeePeekDimens.spacing1 else CoffeePeekDimens.spacing2)) {
        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            if (imageContent == null) Icon(CoffeePeekIcons.Coffee, null, Modifier.size(if (compact) iconSize else 24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else imageContent(Modifier.size(iconSize))
        }
        Text(name, Modifier.weight(1f, fill = false),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            color = if (compact) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Preview @Composable private fun CoffeePeekDrinkLabelLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Column {
        CoffeePeekDrinkLabel("Капучино")
        CoffeePeekDrinkLabel("Капучино", size = CoffeePeekDrinkLabelSize.Compact)
    }
}
@Preview @Composable private fun CoffeePeekDrinkLabelDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Column {
        CoffeePeekDrinkLabel("Капучино")
        CoffeePeekDrinkLabel("Капучино", size = CoffeePeekDrinkLabelSize.Compact)
    }
}
