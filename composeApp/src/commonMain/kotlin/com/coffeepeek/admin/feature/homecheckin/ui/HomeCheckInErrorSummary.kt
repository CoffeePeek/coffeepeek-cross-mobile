package com.coffeepeek.admin.feature.homecheckin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun HomeCheckInErrorSummary(message: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()
        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
        .padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(CpIcons.Error, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Не удалось сохранить чекин", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
