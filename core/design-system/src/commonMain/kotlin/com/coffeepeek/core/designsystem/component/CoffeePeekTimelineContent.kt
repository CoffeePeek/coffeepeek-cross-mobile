package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Timeline geometry only. Features own identities, dates, permissions and all supported actions. */
@Composable
fun CoffeePeekTimelineContent(
    note: String,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    heading: @Composable () -> Unit,
    photos: @Composable () -> Unit,
    supporting: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        header()
        heading()
        if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        photos()
        supporting?.invoke()
        footer?.invoke()
    }
}

@Composable
private fun TimelineContentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekTimelineContent("Сбалансированный кофе с ягодными нотами.",
        header = { CoffeePeekTimelineHeader("Анна", "14 октября 2026", avatar = {}) },
        heading = { Text("CoffeePeek", style = MaterialTheme.typography.titleLarge) },
        photos = {}, footer = { CoffeePeekTimelineFooter(3, true, "Лайки: 3", "Капучино") })
}

@Preview @Composable private fun TimelineContentLightPreview() = TimelineContentPreview(false)
@Preview @Composable private fun TimelineContentDarkPreview() = TimelineContentPreview(true)
