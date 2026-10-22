package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.R

@Composable
internal fun PublicProfileActions(isBlocked: Boolean, onShare: (() -> Unit)?, onBlock: () -> Unit) {
    val shareLabel = stringResource(R.string.profile_share)
    val blockLabel = stringResource(if (isBlocked) R.string.profile_unblock_author else R.string.profile_block_author)
    val actions = buildList {
        if (!isBlocked && onShare != null) add(CoffeePeekMenuAction(shareLabel, CoffeePeekIcons.Share, onClick = onShare))
        add(CoffeePeekMenuAction(blockLabel, CoffeePeekIcons.Block, destructive = !isBlocked, onClick = onBlock))
    }
    CoffeePeekActionMenu(actions) { open ->
        IconButton(onClick = open) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(R.string.profile_author_actions))
        }
    }
}

@PreviewLightDark
@Composable
private fun PublicProfileActionsPreview() = CoffeePeekTheme {
    PublicProfileActions(isBlocked = false, onShare = {}, onBlock = {})
}
