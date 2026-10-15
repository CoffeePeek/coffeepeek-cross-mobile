package com.coffeepeek.admin.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    val items = buildList {
        onEditClick?.let {
            add(PlatformMenuItem("edit", "Редактировать", enabled = enabled, icon = PlatformMenuIcon.Edit, onClick = it))
        }
        onHideClick?.let {
            add(PlatformMenuItem("hide", if (isHiding) "Сохраняем…" else "Скрыть",
                enabled = enabled && !isHiding, icon = PlatformMenuIcon.Hide, onClick = it))
        }
        onReportClick?.let {
            add(PlatformMenuItem("report", "Пожаловаться", enabled = enabled, icon = PlatformMenuIcon.Report, onClick = it))
        }
    }
    PlatformMenuButton(items, contentDescription = "Действия с чекином", enabled = enabled) { openMenu ->
        IconButton(onClick = openMenu, enabled = enabled) {
            Icon(CpIcons.MoreHorizontal, "Действия с чекином", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
