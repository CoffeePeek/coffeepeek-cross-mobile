package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Labeled progress through the steps of a shop check-in. */
@Composable
fun CoffeePeekCheckInProgress(
    step: Int,
    total: Int,
    stepLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(Modifier.widthIn(max = 200.dp).fillMaxWidth().semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(step.toFloat(), 0f..total.toFloat())
            stateDescription = stepLabel
        }, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            repeat(total) { index ->
                val color = if (index < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Preview
@Composable
private fun CoffeePeekCheckInProgressLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekCheckInProgress(2, 3, "Шаг 2 из 3")
}

@Preview
@Composable
private fun CoffeePeekCheckInProgressDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekCheckInProgress(1, 2, "Шаг 1 из 2")
}
