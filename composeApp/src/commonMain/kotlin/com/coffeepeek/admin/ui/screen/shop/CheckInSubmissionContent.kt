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
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.SubmissionStatus
import com.coffeepeek.admin.ui.component.SubmissionStatusContent
import com.coffeepeek.domain.model.CheckInVisibility

@Composable
internal fun CheckInSubmissionContent(
    submittedVisibility: CheckInVisibility?,
    onGoToFeed: () -> Unit,
    onViewCheckIns: () -> Unit,
) {
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
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(CpDimens.spacing6),
        fillAvailableHeight = false,
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
