package com.coffeepeek.feature.community.api

/** Feed and standalone post supply their own draft and actions. */
data class CommunityReportPresentation(
    val title: String, val placeholder: String, val text: String, val limit: Int,
    val error: String?, val submitLabel: String, val cancelLabel: String, val busy: Boolean,
)

class CommunityReportActions(val text: (String) -> Unit, val submit: () -> Unit, val dismiss: () -> Unit)
