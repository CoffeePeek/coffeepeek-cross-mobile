package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInEditorField(label: String, error: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.semantics { error?.let { error(it) } }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        error?.let { HomeCheckInEditorFieldError(it) }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorFieldPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorField(stringResource(R.string.home_check_in_editor_note)) {
        HomeCheckInEditorInput("", {}, stringResource(R.string.home_check_in_editor_note),
            stringResource(R.string.home_check_in_note_hint), true, multiline = true)
    } }
}
