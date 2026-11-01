package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

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
    val localizedLegacyCheckInActionsCommunityBlockAuthor = stringResource(Res.string.legacy_check_in_actions_community_block_author)
    val localizedLegacyCheckInActionsOwnershipEdit = stringResource(Res.string.legacy_check_in_actions_ownership_edit)
    val localizedNotificationsSaving = stringResource(Res.string.notifications_saving)
    val localizedLegacyCheckInActionsCommunityHide = stringResource(Res.string.legacy_check_in_actions_community_hide)
    val localizedLegacyCheckInActionsCommunityReport = stringResource(Res.string.legacy_check_in_actions_community_report)
    val localizedLegacyCheckInActionsCommunityActions = stringResource(Res.string.legacy_check_in_actions_community_actions)

    val blocking = blockActions ?: if (author != null) org.koin.compose.koinInject<com.coffeepeek.feature.authorblocking.api.AuthorBlockActions>() else null
    if (onEditClick == null && onReportClick == null && onHideClick == null && author == null) return
    val items = buildList {
        if (author != null && blocking != null) add(PlatformMenuItem("block", localizedLegacyCheckInActionsCommunityBlockAuthor, enabled = enabled,
            destructive = true, icon = PlatformMenuIcon.Block, onClick = { blocking.requestBlock(author) }))
        onEditClick?.let {
            add(PlatformMenuItem("edit", localizedLegacyCheckInActionsOwnershipEdit, enabled = enabled, icon = PlatformMenuIcon.Edit, onClick = it))
        }
        onHideClick?.let {
            add(PlatformMenuItem("hide", if (isHiding) localizedNotificationsSaving else localizedLegacyCheckInActionsCommunityHide,
                enabled = enabled && !isHiding, icon = PlatformMenuIcon.Hide, onClick = it))
        }
        onReportClick?.let {
            add(PlatformMenuItem("report", localizedLegacyCheckInActionsCommunityReport, enabled = enabled, icon = PlatformMenuIcon.Report, onClick = it))
        }
    }
    PlatformMenuButton(items, contentDescription = localizedLegacyCheckInActionsCommunityActions, enabled = enabled) { openMenu ->
        IconButton(onClick = openMenu, enabled = enabled) {
            Icon(CpIcons.MoreHorizontal, localizedLegacyCheckInActionsCommunityActions, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview @Composable private fun CheckInActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInActions(onEditClick = {}, onReportClick = {})
}
@Preview @Composable private fun CheckInActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInActions(onEditClick = {}, onReportClick = {})
}
