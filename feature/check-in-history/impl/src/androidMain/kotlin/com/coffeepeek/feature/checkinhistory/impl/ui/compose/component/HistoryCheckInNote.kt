package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCheckInNote(id: String, note: String, modifier: Modifier = Modifier) {
    if (note.isBlank()) return
    var expanded by rememberSaveable(id, note) { mutableStateOf(false) }
    var overflows by remember(id, note) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(note, Modifier.animateContentSize(), style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow })
        if (overflows || expanded) Text(
            stringResource(if (expanded) Res.string.history_collapse_note else Res.string.history_expand_note),
            Modifier.clickable(role = Role.Button) { expanded = !expanded }.padding(vertical = CoffeePeekDimens.spacing2),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@PreviewLightDark @Composable private fun HistoryCheckInNotePreview() = CoffeePeekTheme {
    HistoryCheckInNote("preview", "Уютная кофейня и отличный кофе. ".repeat(12))
}
