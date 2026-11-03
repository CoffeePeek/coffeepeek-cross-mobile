package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

enum class ShopReportValidation { ReasonRequired, DescriptionRequired }

@Composable
internal fun ShopReportReason.localizedTitle(): String = when (this) {
    ShopReportReason.OUTDATED_MENU -> stringResource(Res.string.legacy_report_reason_outdated_menu)
    ShopReportReason.SHOP_CLOSED -> stringResource(Res.string.legacy_report_reason_shop_closed)
    ShopReportReason.INCORRECT_SCHEDULE -> stringResource(Res.string.legacy_report_reason_incorrect_schedule)
    ShopReportReason.INCORRECT_PHOTOS -> stringResource(Res.string.legacy_report_reason_incorrect_photos)
    ShopReportReason.INCORRECT_ADDRESS -> stringResource(Res.string.legacy_report_reason_incorrect_address)
    ShopReportReason.OTHER -> stringResource(Res.string.legacy_report_reason_other)
}

internal data class ShopReportValidationLabels(val reasonRequired: String, val descriptionRequired: String)

@Composable
internal fun shopReportValidationLabels() = ShopReportValidationLabels(
    stringResource(Res.string.legacy_report_reason_required),
    stringResource(Res.string.legacy_community_screen_community_report_placeholder),
)

internal fun ShopReportValidation.message(labels: ShopReportValidationLabels): String = when (this) {
    ShopReportValidation.ReasonRequired -> labels.reasonRequired
    ShopReportValidation.DescriptionRequired -> labels.descriptionRequired
}
