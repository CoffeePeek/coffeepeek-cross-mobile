package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** The detail modal shows the full note, just like main's fullVersion card. */
@Composable
internal fun HistoryCheckInNote(note: String, modifier: Modifier = Modifier) {
    if (note.isNotBlank()) Text(note, modifier, style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface)
}

@PreviewLightDark @Composable private fun HistoryCheckInNotePreview() = CoffeePeekTheme {
    HistoryCheckInNote("Уютная кофейня и отличный кофе. ".repeat(4))
}
