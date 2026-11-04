package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.*

@Composable
internal fun PublicProfileActions(isBlocked: Boolean, onShare: (() -> Unit)?, onBlock: () -> Unit) {
    val shareLabel = stringResource(Res.string.profile_share)
    val blockLabel = stringResource(if (isBlocked) Res.string.profile_unblock_author else Res.string.profile_block_author)
    val actions = buildList {
        if (!isBlocked && onShare != null) add(CoffeePeekMenuAction(shareLabel, CoffeePeekIcons.Share, onClick = onShare))
        add(CoffeePeekMenuAction(blockLabel, CoffeePeekIcons.Block, destructive = !isBlocked, onClick = onBlock))
    }
    CoffeePeekActionMenu(actions) { open ->
        IconButton(onClick = open) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.profile_author_actions))
        }
    }
}

@Composable
private fun PublicProfileActionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PublicProfileActions(isBlocked = false, onShare = {}, onBlock = {})
}

@Preview @Composable private fun PublicProfileActionsLightPreview() = PublicProfileActionsPreview(false)
@Preview @Composable private fun PublicProfileActionsDarkPreview() = PublicProfileActionsPreview(true)
