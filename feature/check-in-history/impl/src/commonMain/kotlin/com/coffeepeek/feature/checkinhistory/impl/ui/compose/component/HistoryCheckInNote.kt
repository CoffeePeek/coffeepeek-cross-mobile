package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** The detail modal shows the full note, just like main's fullVersion card. */
@Composable
internal fun HistoryCheckInNote(note: String, modifier: Modifier = Modifier) {
    if (note.isNotBlank()) Text(note, modifier, style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface)
}

@Composable private fun HistoryCheckInNotePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryCheckInNote("Уютная кофейня и отличный кофе. ".repeat(4))
}
@Preview @Composable private fun HistoryCheckInNoteLightPreview() = HistoryCheckInNotePreview(false)
@Preview @Composable private fun HistoryCheckInNoteDarkPreview() = HistoryCheckInNotePreview(true)
