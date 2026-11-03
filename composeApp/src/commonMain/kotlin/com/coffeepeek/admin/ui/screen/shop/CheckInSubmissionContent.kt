package com.coffeepeek.admin.ui.screen.shop

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.SubmissionStatus
import com.coffeepeek.admin.ui.component.SubmissionStatusContent
import com.coffeepeek.domain.model.CheckInVisibility
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInSubmissionContent(
    submittedVisibility: CheckInVisibility?,
    onGoToFeed: () -> Unit,
    onViewCheckIns: () -> Unit,
) {
    val sending = submittedVisibility == null
    val localizedLegacyCheckInSubmissionContentShopCheckinFormSending = stringResource(Res.string.legacy_check_in_submission_content_shop_checkin_form_sending)
    val localizedLegacyCheckInSubmissionContentShopCheckinFormSuccess = stringResource(Res.string.legacy_check_in_submission_content_shop_checkin_form_success)
    val localizedLegacyCheckInSubmissionContentSavingCoffeeMoment = stringResource(Res.string.legacy_check_in_submission_content_saving_coffee_moment)
    val localizedLegacyCheckInSubmissionContentShopCheckinFormSuccessPublic = stringResource(Res.string.legacy_check_in_submission_content_shop_checkin_form_success_public)
    val localizedLegacyCheckInSubmissionContentShopCheckinFormSuccessPrivate = stringResource(Res.string.legacy_check_in_submission_content_shop_checkin_form_success_private)
    val localizedLegacyCheckInSubmissionContentShopCheckinFormFeed = stringResource(Res.string.legacy_check_in_submission_content_shop_checkin_form_feed)
    val localizedLegacyCheckInSubmissionContentShopCheckinsTitle = stringResource(Res.string.legacy_check_in_submission_content_shop_checkins_title)

    SubmissionStatusContent(
        status = if (submittedVisibility == null) SubmissionStatus.Sending else SubmissionStatus.Success,
        title = if (submittedVisibility == null) localizedLegacyCheckInSubmissionContentShopCheckinFormSending else localizedLegacyCheckInSubmissionContentShopCheckinFormSuccess,
        description = when (submittedVisibility) {
            null -> localizedLegacyCheckInSubmissionContentSavingCoffeeMoment
            CheckInVisibility.Public -> localizedLegacyCheckInSubmissionContentShopCheckinFormSuccessPublic
            else -> localizedLegacyCheckInSubmissionContentShopCheckinFormSuccessPrivate
        },
        modifier = Modifier.fillMaxWidth().then(if (sending) Modifier.fillMaxHeight() else Modifier)
            .navigationBarsPadding().padding(CpDimens.spacing6),
        fillAvailableHeight = sending,
    ) {
        AppButton(localizedLegacyCheckInSubmissionContentShopCheckinFormFeed, onClick = onGoToFeed)
        OutlinedButton(
            onClick = onViewCheckIns,
            modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.buttonHeight),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text(localizedLegacyCheckInSubmissionContentShopCheckinsTitle)
        }
    }
}

@Composable private fun CheckInSendingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.size(390.dp, 600.dp)) { CheckInSubmissionContent(null, {}, {}) }
}

@Preview @Composable private fun CheckInSendingLightPreview() = CheckInSendingPreview(false)
@Preview @Composable private fun CheckInSendingDarkPreview() = CheckInSendingPreview(true)
