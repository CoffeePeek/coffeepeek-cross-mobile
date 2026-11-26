package com.coffeepeek.feature.community.impl.ui

import com.coffeepeek.feature.community.api.CommunityReportActions
import com.coffeepeek.feature.community.api.CommunityReportPresentation
import com.coffeepeek.feature.community.domain.rules.MAX_REPORT_LENGTH

internal data class NativeReportTexts(val title: String, val placeholder: String, val issue: String,
    val failed: String, val sending: String, val send: String, val cancel: String)

internal fun nativeReportPresentation(form: ReportForm, texts: NativeReportTexts) = CommunityReportPresentation(
    title = texts.title, placeholder = texts.placeholder, text = form.text, limit = MAX_REPORT_LENGTH,
    error = when {
        form.issue != null -> texts.issue
        form.failed -> form.failure ?: texts.failed
        else -> null
    },
    submitLabel = if (form.isSending) texts.sending else texts.send, cancelLabel = texts.cancel, busy = form.isSending,
)

internal fun nativeReportActions(callbacks: ReportCallbacks) = CommunityReportActions(
    text = { callbacks.onText(it.take(MAX_REPORT_LENGTH)) }, submit = callbacks.onSubmit, dismiss = callbacks.onDismiss,
)
