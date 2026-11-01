package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

internal data class PreparationMetricLabels(val grams: String, val seconds: String, val minutes: String, val hours: String, val otherMethod: String)

@Composable
internal fun preparationMetricLabels() = PreparationMetricLabels(
    stringResource(Res.string.legacy_grams), stringResource(Res.string.legacy_seconds),
    stringResource(Res.string.legacy_minutes), stringResource(Res.string.legacy_hours),
    stringResource(Res.string.select_drink_other),
)
