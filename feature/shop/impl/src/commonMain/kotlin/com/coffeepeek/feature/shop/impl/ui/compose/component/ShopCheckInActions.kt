package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsRow
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsDivider
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
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
    var expanded by remember(onReport != null, onBlockAuthor != null) { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.shop_checkins_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp, max = if (onBlockAuthor != null) 320.dp else 280.dp),
            shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp, shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            onBlockAuthor?.let { block ->
                CoffeePeekSettingsRow(CoffeePeekIcons.Block, stringResource(Res.string.shop_checkins_block_author),
                    onClick = { expanded = false; block() }, showArrow = false,
                    iconColors = CoffeePeekIconBadgePalette.Rose)
                if (onReport != null) CoffeePeekSettingsDivider()
            }
            onReport?.let { report ->
                CoffeePeekSettingsRow(CoffeePeekIcons.Error, stringResource(Res.string.shop_checkins_report),
                    onClick = { expanded = false; report() }, showArrow = false,
                    iconColors = CoffeePeekIconBadgePalette.Rose)
            }
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
