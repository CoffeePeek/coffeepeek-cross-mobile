package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun CheckInActions(
    onEditClick: (() -> Unit)?,
    onReportClick: (() -> Unit)?,
    onHideClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    isHiding: Boolean = false,
) {
    if (onEditClick == null && onReportClick == null && onHideClick == null) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(CpIcons.MoreHorizontal, "Действия с чекином", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
            shape = RoundedCornerShape(CpDimens.radius2xl),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            onEditClick?.let { edit ->
                SettingsRow(CpIcons.Edit, "Редактировать", onClick = { expanded = false; edit() },
                    showArrow = false, iconColors = SettingsIconPalette.Gold, enabled = enabled)
            }
            onHideClick?.let { hide ->
                SettingsRow(CpIcons.VisibilityOff, if (isHiding) "Сохраняем…" else "Скрыть", onClick = { expanded = false; hide() },
                    showArrow = false, iconColors = SettingsIconPalette.Lavender, enabled = enabled)
            }
            onReportClick?.let { report ->
                SettingsRow(CpIcons.Error, "Пожаловаться", onClick = { expanded = false; report() },
                    showArrow = false, iconColors = SettingsIconPalette.Rose, enabled = enabled)
            }
        }
    }
}
