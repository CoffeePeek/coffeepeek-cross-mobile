package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun InfoChip(text: String) {
    Box(
        modifier = Modifier.widthIn(max = 112.dp)
            .clip(RoundedCornerShape(CpDimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(CpDimens.radiusLg))
            .padding(horizontal = CpDimens.spacing2, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Preview @Composable private fun InfoChipLightPreview() = CoffeePeekTheme(darkTheme = false) {
    InfoChip("Эспрессо")
}
@Preview @Composable private fun InfoChipDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    InfoChip("Эспрессо")
}
