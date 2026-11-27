package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_editor_note
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_note_hint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HomeCheckInEditorField(label: String, error: String? = null,
    counter: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.semantics { error?.let { error(it) } }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) {
            if (counter == null) Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(counter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
        error?.let { HomeCheckInEditorFieldError(it) }
    }
}

@Composable private fun HomeCheckInEditorFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorField(stringResource(Res.string.home_check_in_editor_note), counter = "0/1000") {
        HomeCheckInEditorInput("", {}, stringResource(Res.string.home_check_in_editor_note),
            stringResource(Res.string.home_check_in_note_hint), true, multiline = true)
    } }
}
@Preview @Composable private fun HomeCheckInEditorFieldLightPreview() = HomeCheckInEditorFieldPreview(false)
@Preview @Composable private fun HomeCheckInEditorFieldDarkPreview() = HomeCheckInEditorFieldPreview(true)
