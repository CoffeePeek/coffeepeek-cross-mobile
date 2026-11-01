package com.coffeepeek.admin.ui.component

import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun CheckInActions(
    onEditClick: (() -> Unit)?,
    onReportClick: (() -> Unit)?,
    onHideClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    isHiding: Boolean = false,
    author: com.coffeepeek.feature.authorblocking.api.AuthorReference? = null,
    blockActions: com.coffeepeek.feature.authorblocking.api.AuthorBlockActions? = null,
) {
    val blocking = blockActions ?: if (author != null) org.koin.compose.koinInject<com.coffeepeek.feature.authorblocking.api.AuthorBlockActions>() else null
    if (onEditClick == null && onReportClick == null && onHideClick == null && author == null) return
    val items = buildList {
        if (author != null && blocking != null) add(PlatformMenuItem("block", "Заблокировать автора", enabled = enabled,
            destructive = true, icon = PlatformMenuIcon.Block, onClick = { blocking.requestBlock(author) }))
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

@Preview @Composable private fun CheckInActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInActions(onEditClick = {}, onReportClick = {})
}
@Preview @Composable private fun CheckInActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInActions(onEditClick = {}, onReportClick = {})
}
