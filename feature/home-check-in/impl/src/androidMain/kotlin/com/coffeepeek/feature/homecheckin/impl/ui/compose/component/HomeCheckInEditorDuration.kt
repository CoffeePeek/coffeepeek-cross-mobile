package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInEditorDuration(seconds: Int?, enabled: Boolean, error: String?, onChange: (Int?) -> Unit) {
    // Preserve a partially entered "0"/empty field while the parent echoes the reduced total.
    var minutesText by remember { mutableStateOf(seconds?.div(60)?.takeIf { it > 0 }?.toString().orEmpty()) }
    var secondsText by remember { mutableStateOf(seconds?.rem(60)?.takeIf { it > 0 }?.toString().orEmpty()) }
    var lastEmitted by remember { mutableStateOf(seconds) }
    LaunchedEffect(seconds) {
        if (seconds != lastEmitted) {
            minutesText = seconds?.div(60)?.takeIf { it > 0 }?.toString().orEmpty()
            secondsText = seconds?.rem(60)?.takeIf { it > 0 }?.toString().orEmpty()
            lastEmitted = seconds
        }
    }
    fun emit() {
        lastEmitted = if (minutesText.isEmpty() && secondsText.isEmpty()) null
            else (minutesText.toIntOrNull() ?: 0) * 60 + (secondsText.toIntOrNull() ?: 0)
        onChange(lastEmitted)
    }
    HomeCheckInEditorField(stringResource(R.string.home_check_in_duration), error) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_check_in_duration_minutes), style = MaterialTheme.typography.bodySmall)
                HomeCheckInEditorInput(minutesText, { minutesText = it; emit() },
                    stringResource(R.string.home_check_in_duration_minutes_description), "0", enabled, error,
                    keyboardType = KeyboardType.Number, maximum = 10080, maxLength = 8)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_check_in_duration_seconds), style = MaterialTheme.typography.bodySmall)
                HomeCheckInEditorInput(secondsText, { secondsText = it; emit() },
                    stringResource(R.string.home_check_in_duration_seconds_description), "00", enabled, error,
                    keyboardType = KeyboardType.Number, maximum = 59, maxLength = 8)
            }
        }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorDurationPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorDuration(180, true, null, {}) }
}
