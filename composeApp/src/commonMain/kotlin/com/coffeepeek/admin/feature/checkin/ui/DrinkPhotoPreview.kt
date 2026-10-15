package com.coffeepeek.admin.feature.checkin.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.DrinkCutoutImage
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoState

/** Public UI entry point so both check-in forms keep the same selection and recovery flow. */
@Composable
internal fun DrinkPhotoPreviewContent(
    state: DrinkPhotoState,
    onSelect: (PendingPhotoUpload) -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Чашка для календаря", style = MaterialTheme.typography.labelLarge)
        Text(
            when {
                state.processing -> "Убираем фон с первого фото…"
                state.skipped -> "Чекин сохранится без отдельного изображения чашки."
                state.error != null -> state.error
                state.candidates.size > 1 && state.selected == null -> "Выберите чашку среди найденных объектов."
                else -> "Проверьте вырезку. Полная фотография тоже сохранится."
            },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.processing) CoffeePeekLoader(size = 24.dp, strokeWidth = 2.dp)
        if (state.candidates.isNotEmpty()) Row(
            Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.candidates.forEachIndexed { index, photo ->
                DrinkCutoutImage(
                    data = photo.bytes,
                    modifier = Modifier.size(96.dp)
                        .border(if (state.selected == photo) 2.dp else 1.dp,
                            if (state.selected == photo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp))
                        .selectable(selected = state.selected == photo, role = Role.RadioButton) { onSelect(photo) }
                        .semantics { contentDescription = "Объект ${index + 1}: выбрать как чашку" }
                        .padding(8.dp),
                )
            }
        }
        Row {
            if (!state.processing && (state.error != null || state.skipped)) TextButton(onClick = onRetry) {
                Text("Повторить выделение")
            }
            if (!state.skipped) TextButton(onClick = onSkip) { Text("Без вырезки") }
        }
    }
}
