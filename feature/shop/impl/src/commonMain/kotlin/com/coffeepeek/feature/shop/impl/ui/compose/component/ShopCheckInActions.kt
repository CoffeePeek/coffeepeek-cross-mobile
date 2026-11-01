package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_actions
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_report
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_block_author
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInActions(onReport: (() -> Unit)?, modifier: Modifier = Modifier, onBlockAuthor: (() -> Unit)? = null) {
    if (onReport == null && onBlockAuthor == null) return
    val actions = buildList {
        onBlockAuthor?.let { add(CoffeePeekMenuAction(stringResource(Res.string.shop_checkins_block_author),
            CoffeePeekIcons.Block, destructive = true, onClick = it)) }
        onReport?.let { add(CoffeePeekMenuAction(stringResource(Res.string.shop_checkins_report),
            CoffeePeekIcons.Error, onClick = it)) }
    }
    CoffeePeekActionMenu(actions, modifier) { open ->
        IconButton(onClick = open) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.shop_checkins_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// The menu itself requires interactive preview; static fixtures display its trigger only.
@Preview @Composable private fun ShopCheckInActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInActions({}, onBlockAuthor = {})
}
@Preview @Composable private fun ShopCheckInActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInActions({}, onBlockAuthor = {})
}
