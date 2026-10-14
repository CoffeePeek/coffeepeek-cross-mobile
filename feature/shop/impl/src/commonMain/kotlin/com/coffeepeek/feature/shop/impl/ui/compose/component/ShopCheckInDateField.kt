package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
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
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(stringResource(Res.string.shop_checkin_form_date), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, role = Role.Button) { shown = true }
            .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
            Icon(CoffeePeekIcons.Calendar, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f))
            Icon(CoffeePeekIcons.ChevronDown, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
