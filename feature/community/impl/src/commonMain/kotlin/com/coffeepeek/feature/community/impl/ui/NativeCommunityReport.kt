package com.coffeepeek.feature.community.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.community.api.CommunityReportActions
import com.coffeepeek.feature.community.api.CommunityReportPresentation
import com.coffeepeek.feature.community.domain.rules.MAX_REPORT_LENGTH
import com.coffeepeek.feature.community.impl.resources.*

@Composable
internal fun nativeReportPresentation(form: ReportForm) = CommunityReportPresentation(
    title = stringResource(Res.string.community_report_title),
    placeholder = stringResource(Res.string.community_report_placeholder), text = form.text, limit = MAX_REPORT_LENGTH,
    error = when {
        form.issue != null -> stringResource(Res.string.community_report_issue)
        form.failed -> form.failure ?: stringResource(Res.string.community_report_failed)
        else -> null
    },
    submitLabel = stringResource(if (form.isSending) Res.string.community_report_sending else Res.string.community_report_send),
    cancelLabel = stringResource(Res.string.community_cancel), busy = form.isSending,
)

internal fun nativeReportActions(callbacks: ReportCallbacks) = CommunityReportActions(
    text = { callbacks.onText(it.take(MAX_REPORT_LENGTH)) }, submit = callbacks.onSubmit, dismiss = callbacks.onDismiss,
)
