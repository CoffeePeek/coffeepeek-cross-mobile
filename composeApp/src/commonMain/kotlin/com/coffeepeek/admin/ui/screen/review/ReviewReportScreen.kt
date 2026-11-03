package com.coffeepeek.admin.ui.screen.review

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReviewReportScreen(reviewId: String, isPreview: Boolean = false, isCheckIn: Boolean = false) {
    val localizedLegacyReviewReportScreenCommunityReportTitle = stringResource(Res.string.legacy_review_report_screen_community_report_title)
    val localizedLegacyReviewReportScreenReportPreview = stringResource(Res.string.legacy_review_report_screen_report_preview)
    val localizedLegacyReviewReportScreenCommunityReportSent = stringResource(Res.string.legacy_review_report_screen_community_report_sent)
    val localizedLegacyReviewReportScreenDemoReportCompleted = stringResource(Res.string.legacy_review_report_screen_demo_report_completed)
    val localizedLegacyReviewReportScreenReportSubmitted = stringResource(Res.string.legacy_review_report_screen_report_submitted)
    val localizedStepDone = stringResource(Res.string.step_done)
    val localizedLegacyReviewReportScreenDemoReportMessage = stringResource(Res.string.legacy_review_report_screen_demo_report_message)
    val localizedLegacyReviewReportScreenCheckInReportPlaceholder = stringResource(Res.string.legacy_review_report_screen_check_in_report_placeholder)
    val localizedLegacyReviewReportScreenCheckInReportQuestion = stringResource(Res.string.legacy_review_report_screen_check_in_report_question)
    val localizedLegacyReviewReportScreenCheckForm = stringResource(Res.string.legacy_review_report_screen_check_form)
    val localizedLegacyReviewReportScreenCommunityReportSend = stringResource(Res.string.legacy_review_report_screen_community_report_send)

    val vm: ReviewReportViewModel = platformViewModel(parameters = { parametersOf(reviewId, isPreview, isCheckIn) })
    val state by vm.state.collectAsState()
    Scaffold(topBar = { CpTopBar(localizedLegacyReviewReportScreenCommunityReportTitle) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).coffeePeekScreenBottomPadding().padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            if (state.isSubmitted) {
                Text(if (state.isPreview) localizedLegacyReviewReportScreenReportPreview else localizedLegacyReviewReportScreenCommunityReportSent, style = MaterialTheme.typography.titleMedium)
                Text(if (state.isPreview) localizedLegacyReviewReportScreenDemoReportCompleted else localizedLegacyReviewReportScreenReportSubmitted)
                Button(onClick = Navigator::popBack, modifier = Modifier.fillMaxWidth()) { Text(localizedStepDone) }
                return@Column
            }
            if (state.isPreview) Text(
                localizedLegacyReviewReportScreenDemoReportMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(localizedLegacyReviewReportScreenCheckInReportPlaceholder, style = MaterialTheme.typography.titleMedium)
            ReviewTextInput(
                value = state.text, onValueChange = vm::updateText,
                placeholder = localizedLegacyReviewReportScreenCheckInReportQuestion, maxLength = 2000,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::submit, enabled = !state.isSubmitting && state.text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                if (state.isSubmitting) CoffeePeekLoader() else Text(if (state.isPreview) localizedLegacyReviewReportScreenCheckForm else localizedLegacyReviewReportScreenCommunityReportSend)
            }
        }
    }
}
