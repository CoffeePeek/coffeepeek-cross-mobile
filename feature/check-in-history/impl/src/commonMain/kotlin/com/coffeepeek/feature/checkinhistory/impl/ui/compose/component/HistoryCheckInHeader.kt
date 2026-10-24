package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.Res
import com.coffeepeek.feature.checkinhistory.impl.resources.history_you
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCheckInHeader(username: String, dateLabel: String, modifier: Modifier = Modifier,
    avatarUrl: String? = null, imageContent: HistoryImageContent? = null) {
    val name = username.ifBlank { stringResource(Res.string.history_you) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
            .clearAndSetSemantics {},
            contentAlignment = Alignment.Center) {
            Text(name.trim().first().uppercase(), style = MaterialTheme.typography.titleMedium)
            if (!avatarUrl.isNullOrBlank() && imageContent != null && !LocalInspectionMode.current)
                imageContent(avatarUrl, Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (dateLabel.isNotBlank()) Text(dateLabel, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview @Composable private fun HistoryCheckInHeaderPreview() = CoffeePeekTheme {
    HistoryCheckInHeader("Анна", "14 октября 2026")
}
