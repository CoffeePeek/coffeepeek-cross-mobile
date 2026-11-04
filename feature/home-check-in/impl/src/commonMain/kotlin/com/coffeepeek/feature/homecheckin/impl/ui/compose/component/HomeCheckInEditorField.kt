package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomeCheckInEditorField(label: String, error: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.semantics { error?.let { error(it) } }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        error?.let { HomeCheckInEditorFieldError(it) }
    }
}

@Composable private fun HomeCheckInEditorFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorField(stringResource(Res.string.home_check_in_editor_note)) {
        HomeCheckInEditorInput("", {}, stringResource(Res.string.home_check_in_editor_note),
            stringResource(Res.string.home_check_in_note_hint), true, multiline = true)
    } }
}
@Preview @Composable private fun HomeCheckInEditorFieldLightPreview() = HomeCheckInEditorFieldPreview(false)
@Preview @Composable private fun HomeCheckInEditorFieldDarkPreview() = HomeCheckInEditorFieldPreview(true)
