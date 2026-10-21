package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Read-only value/caption pair with an optional leading visual, no fabricated click action. */
@Composable
fun CoffeePeekInfoItem(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.titleSmall)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CoffeePeekInfoItemPreviewContent(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        CoffeePeekInfoItem("Medium", "Roast", leading = {
            CoffeePeekIconBadge(CoffeePeekIcons.Fire, CoffeePeekIconBadgePalette.Gold)
        })
    }
}

@Preview @Composable private fun CoffeePeekInfoItemLightPreview() = CoffeePeekInfoItemPreviewContent(false)
@Preview @Composable private fun CoffeePeekInfoItemDarkPreview() = CoffeePeekInfoItemPreviewContent(true)
