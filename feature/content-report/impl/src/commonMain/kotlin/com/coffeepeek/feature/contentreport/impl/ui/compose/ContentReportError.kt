package com.coffeepeek.feature.contentreport.impl.ui.compose

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.contentreport.domain.model.ContentReportProblem
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.ContentReportState
import com.coffeepeek.feature.contentreport.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun reportError(state: ContentReportState): String {
    val problem = state.error?.problem
    state.error?.serverMessage?.takeIf(String::isNotBlank)?.let { return it }
    return stringResource(when (problem) {
        ContentReportProblem.InvalidInput -> Res.string.content_report_validation
        ContentReportProblem.SignInRequired -> if (state.isLoggedIn && state.isCheckIn) Res.string.content_report_check_in_sign_in else Res.string.content_report_sign_in
        ContentReportProblem.Forbidden -> if (state.isCheckIn) Res.string.content_report_forbidden else Res.string.content_report_failed
        ContentReportProblem.NotFound -> if (state.isCheckIn) Res.string.content_report_check_in_missing else Res.string.content_report_review_missing
        ContentReportProblem.RateLimited -> if (state.isCheckIn) Res.string.content_report_check_in_failed else Res.string.content_report_rate_limit
        else -> if (state.isCheckIn) Res.string.content_report_check_in_failed else Res.string.content_report_failed
    })
}
