package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.material3.Icon
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_author_actions
import com.coffeepeek.feature.profile.impl.resources.profile_block_author
import com.coffeepeek.feature.profile.impl.resources.profile_share
import com.coffeepeek.feature.profile.impl.resources.profile_unblock_author
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun PublicProfileActions(isBlocked: Boolean, onShare: (() -> Unit)?, onBlock: () -> Unit) {
    val shareLabel = stringResource(Res.string.profile_share)
    val blockLabel = stringResource(if (isBlocked) Res.string.profile_unblock_author else Res.string.profile_block_author)
    val actions = buildList {
        if (!isBlocked && onShare != null) add(CoffeePeekMenuAction(shareLabel, CoffeePeekIcons.Share, onClick = onShare))
        add(CoffeePeekMenuAction(blockLabel, CoffeePeekIcons.Block, destructive = !isBlocked, nativeIcon = CoffeePeekMenuIcon.Block, onClick = onBlock))
    }
    CoffeePeekActionMenu(actions, menuDescription = stringResource(Res.string.profile_author_actions)) { open ->
        IconButton(onClick = open) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.profile_author_actions))
        }
    }
}

@Preview
@Composable
private fun PublicProfileActionsPreview() = CoffeePeekTheme {
    PublicProfileActions(isBlocked = false, onShare = {}, onBlock = {})
}
