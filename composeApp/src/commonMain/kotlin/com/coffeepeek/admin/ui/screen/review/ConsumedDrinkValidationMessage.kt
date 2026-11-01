package com.coffeepeek.admin.ui.screen.review

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.domain.model.ConsumedDrinkIssue
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

internal data class ConsumedDrinkValidationLabels(val customNameLength: String, val customNameOnlyForOther: String)

@Composable
internal fun consumedDrinkValidationLabels() = ConsumedDrinkValidationLabels(
    stringResource(Res.string.legacy_drink_custom_name_length),
    stringResource(Res.string.legacy_drink_custom_name_other),
)

internal fun ConsumedDrinkIssue.message(labels: ConsumedDrinkValidationLabels): String = when (this) {
    ConsumedDrinkIssue.CustomNameLength -> labels.customNameLength
    ConsumedDrinkIssue.CustomNameOnlyForOther -> labels.customNameOnlyForOther
}
