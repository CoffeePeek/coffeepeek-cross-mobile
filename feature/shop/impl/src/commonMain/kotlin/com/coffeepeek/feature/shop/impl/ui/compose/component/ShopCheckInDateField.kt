package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_date
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_date_confirm
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_cancel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShopCheckInDateField(
    label: String, selectedMillis: Long?, nowMillis: Long, enabled: Boolean, onChange: (Long) -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { shown = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.shop_checkin_form_date) + ": " + label)
    }
    if (shown && enabled) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis > 0 && utcTimeMillis <= nowMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { shown = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = { state.selectedDateMillis?.let(onChange); shown = false },
                ) { Text(stringResource(Res.string.shop_checkin_form_date_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { shown = false }) { Text(stringResource(Res.string.shop_checkin_form_cancel)) }
            },
        ) { DatePicker(state, showModeToggle = false) }
    }
}

@Preview @Composable private fun ShopCheckInDateFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInDateField("9 октября 2026", 1791504000000, 1791547200000, true, {})
}
@Preview @Composable private fun ShopCheckInDateFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInDateField("9 октября 2026", 1791504000000, 1791547200000, true, {})
}
