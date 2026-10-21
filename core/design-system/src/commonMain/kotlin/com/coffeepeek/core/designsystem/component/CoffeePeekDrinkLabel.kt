package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

/** Read-only drink image and name: no plate, border, chevron or implied action.
 * The owner supplies an image renderer; this component never loads a URL itself.
 */
@Composable
fun CoffeePeekDrinkLabel(
    name: String,
    modifier: Modifier = Modifier,
    imageContent: (@Composable (Modifier) -> Unit)? = null,
) {
    Row(modifier.widthIn(max = 200.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (imageContent == null) Icon(CoffeePeekIcons.Coffee, null, Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else imageContent(Modifier.size(32.dp))
        }
        Text(name, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Preview @Composable private fun CoffeePeekDrinkLabelLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekDrinkLabel("Капучино")
}
@Preview @Composable private fun CoffeePeekDrinkLabelDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekDrinkLabel("Капучино")
}
