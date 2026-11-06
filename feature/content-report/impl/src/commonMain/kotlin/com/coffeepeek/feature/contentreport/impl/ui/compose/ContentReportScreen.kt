package com.coffeepeek.feature.contentreport.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import com.coffeepeek.core.designsystem.theme.*
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.*
import com.coffeepeek.feature.contentreport.impl.ui.compose.component.ContentReportTextField
import com.coffeepeek.feature.contentreport.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContentReportScreenContent(state: ContentReportState, onAction: (ContentReportAction) -> Unit) {
    CoffeePeekBackHandler { onAction(ContentReportAction.Back()) }
    Scaffold(topBar = { CoffeePeekTopBar(stringResource(Res.string.content_report_title),
        stringResource(Res.string.content_report_back), onBack = { onAction(ContentReportAction.Back()) }) }) { insets ->
        CoffeePeekScrollableColumn(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)
            .coffeePeekScreenBottomPadding().padding(CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            when {
                !state.accessAvailable -> {
                    Text(stringResource(Res.string.content_report_access_failed))
                    CoffeePeekButton(stringResource(Res.string.content_report_retry), { onAction(ContentReportAction.RetryAccess()) })
                }
                state.deliveryUnconfirmed -> {
                    Text(stringResource(Res.string.content_report_unconfirmed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CoffeePeekButton(stringResource(Res.string.content_report_back), { onAction(ContentReportAction.Back()) })
                }
                state.isSubmitted -> ReportConfirmation(state, onAction)
                else -> ReportForm(state, onAction)
            }
        }
    }
}

@Composable
private fun ReportConfirmation(state: ContentReportState, onAction: (ContentReportAction) -> Unit) {
    Text(stringResource(if (state.isPreview) Res.string.content_report_preview_title else Res.string.content_report_success_title),
        style = MaterialTheme.typography.titleMedium)
    Text(stringResource(if (state.isPreview) Res.string.content_report_preview_success else Res.string.content_report_success))
    CoffeePeekButton(stringResource(Res.string.content_report_done), { onAction(ContentReportAction.Back()) })
}

@Composable
private fun ReportForm(state: ContentReportState, onAction: (ContentReportAction) -> Unit) {
    if (state.isPreview) Text(stringResource(Res.string.content_report_preview_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(stringResource(Res.string.content_report_prompt), style = MaterialTheme.typography.titleMedium)
    ContentReportTextField(state, { onAction(ContentReportAction.TextChanged(it)) })
    state.error?.let { Text(reportError(state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    CoffeePeekButton(stringResource(if (state.isPreview) Res.string.content_report_preview_submit else Res.string.content_report_submit),
        { onAction(ContentReportAction.Submit()) }, enabled = !state.isSubmitting && state.text.isNotBlank(),
        leadingIcon = if (state.isSubmitting) ({ CoffeePeekLoader(stringResource(Res.string.content_report_loading),
            size = 24.dp, color = LocalContentColor.current) }) else null)
}

@Composable private fun ReportPreview(dark: Boolean, preview: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ContentReportScreenContent(ContentReportState(isPreview = preview), {})
}
@Preview @Composable private fun ReportLightPreview() = ReportPreview(false, false)
@Preview @Composable private fun ReportDarkPreview() = ReportPreview(true, false)
@Preview @Composable private fun ExampleReportLightPreview() = ReportPreview(false, true)
@Preview @Composable private fun ExampleReportDarkPreview() = ReportPreview(true, true)
