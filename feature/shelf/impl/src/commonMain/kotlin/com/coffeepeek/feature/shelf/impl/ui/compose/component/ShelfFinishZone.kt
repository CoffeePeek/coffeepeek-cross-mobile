package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_finish_zone
import com.coffeepeek.feature.shelf.impl.resources.shelf_finish_zone_hint
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Drop target shown while a bag is lifted; [highlighted] when the bag is over it. */
@Composable
internal fun ShelfFinishZone(highlighted: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        color = if (highlighted) colors.primaryContainer else colors.surface, shadowElevation = 6.dp,
        border = BorderStroke(1.5.dp, if (highlighted) colors.primary else colors.outlineVariant)) {
        Row(Modifier.padding(CoffeePeekDimens.cardPadding), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Icon(CoffeePeekIcons.CheckCircle, null, tint = if (highlighted) colors.primary else colors.onSurfaceVariant)
            Column {
                Text(stringResource(Res.string.shelf_finish_zone), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(Res.string.shelf_finish_zone_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ShelfFinishZonePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShelfFinishZone(highlighted = false)
        ShelfFinishZone(highlighted = true)
    }
}

@Preview @Composable private fun ShelfFinishZoneLightPreview() = ShelfFinishZonePreview(false)
@Preview @Composable private fun ShelfFinishZoneDarkPreview() = ShelfFinishZonePreview(true)
