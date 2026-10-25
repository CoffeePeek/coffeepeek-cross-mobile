package com.coffeepeek.feature.contentreport.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCompactOutlinedTextField
import com.coffeepeek.core.designsystem.theme.*
import com.coffeepeek.feature.contentreport.domain.model.ContentReportProblem
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.ContentReportState
import com.coffeepeek.feature.contentreport.impl.ui.compose.reportError
import com.coffeepeek.feature.contentreport.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContentReportTextField(state: ContentReportState, onText: (String) -> Unit) {
    val invalid = state.error?.problem == ContentReportProblem.InvalidInput
    val background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    CoffeePeekCompactOutlinedTextField(state.text, onText, Modifier.fillMaxWidth(), enabled = state.canEdit,
        singleLine = false, shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius), contentPadding = PaddingValues(14.dp, 12.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        placeholder = { Text(stringResource(Res.string.content_report_placeholder), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        isError = invalid, errorDescription = if (invalid) reportError(state) else null,
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = background, unfocusedContainerColor = background,
            disabledContainerColor = background, errorContainerColor = background,
            focusedBorderColor = MaterialTheme.colorScheme.outline, unfocusedBorderColor = MaterialTheme.colorScheme.outline))
}

@Composable private fun ReportFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ContentReportTextField(ContentReportState(text = "Пример причины жалобы"), {})
}
@Preview @Composable private fun ReportFieldLightPreview() = ReportFieldPreview(false)
@Preview @Composable private fun ReportFieldDarkPreview() = ReportFieldPreview(true)
