package com.coffeepeek.admin.feature.checkin.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PickedImage

@Composable
internal fun DrinkPhotoPreview(
    selected: PickedImage?,
    candidates: List<PickedImage>,
    isProcessing: Boolean,
    skipped: Boolean,
    error: String?,
    onSelect: (PickedImage) -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Чашка для календаря", style = MaterialTheme.typography.labelLarge)
        Text(
            when {
                isProcessing -> "Убираем фон с первого фото…"
                skipped -> "Чекин будет сохранён без отдельного изображения чашки."
                error != null -> error
                candidates.size > 1 -> "Выберите чашку среди найденных объектов."
                else -> "Проверьте, что на вырезке чашка. Полное фото тоже сохранится."
            },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isProcessing) CoffeePeekLoader(size = 24.dp, strokeWidth = 2.dp)
        val previews = candidates.ifEmpty { listOfNotNull(selected) }
        if (previews.isNotEmpty()) Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            previews.forEachIndexed { index, photo ->
                CpImage(
                    data = photo.bytes,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp)
                        .border(
                            if (selected == photo) 2.dp else 1.dp,
                            if (selected == photo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp),
                        )
                        .selectable(selected = selected == photo, role = Role.RadioButton) { onSelect(photo) }
                        .semantics { contentDescription = "Объект ${index + 1}: выбрать как чашку" }
                        .padding(8.dp),
                )
            }
        }
        Row {
            if (!isProcessing && (error != null || skipped)) TextButton(onClick = onRetry) {
                Text("Повторить выделение")
            }
            if (!skipped) TextButton(onClick = onSkip) { Text("Без вырезки") }
        }
    }
}
