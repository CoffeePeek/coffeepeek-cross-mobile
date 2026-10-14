package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInHeader(author: String, dateLabel: String, onReport: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(CoffeePeekColors.GoldWarmSoft)
            .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(author.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = CoffeePeekColors.LightTextPrimary)
        }
        Column(Modifier.weight(1f).padding(start = CoffeePeekDimens.spacing3),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(author, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (dateLabel.isNotBlank()) Text(dateLabel, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onReport != null) ShopCheckInActions(onReport)
    }
}

@Preview @Composable private fun ShopCheckInHeaderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInHeader("Алексей", "1 окт. 2026", {})
}
@Preview @Composable private fun ShopCheckInHeaderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInHeader("Алексей", "1 окт. 2026", null)
}
