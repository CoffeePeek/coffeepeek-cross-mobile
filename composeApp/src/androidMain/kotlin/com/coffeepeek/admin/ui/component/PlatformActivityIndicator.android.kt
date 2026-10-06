package com.coffeepeek.admin.ui.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal actual fun PlatformActivityIndicator(modifier: Modifier) {
    CircularProgressIndicator(
        modifier = modifier.semantics { contentDescription = "Проверка обновлений" },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        strokeWidth = 2.dp,
    )
}
