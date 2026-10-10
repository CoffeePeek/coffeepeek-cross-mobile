package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_share
import com.coffeepeek.feature.shop.impl.resources.shop_detail_suggest_change
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopHeaderActions(
    isFavorite: Boolean,
    favoriteAvailable: Boolean,
    isFavoriteLoading: Boolean,
    onSuggestChange: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Row {
        IconButton(onClick = onSuggestChange) {
            Icon(CpIcons.NoteEdit, contentDescription = stringResource(Res.string.shop_detail_suggest_change))
        }
        ShopFavoriteButton(isFavorite, favoriteAvailable, isFavoriteLoading, onToggleFavorite)
        IconButton(onClick = onShare) {
            Icon(CpIcons.Share, contentDescription = stringResource(Res.string.shop_detail_share))
        }
    }
}

@Preview @Composable private fun ShopHeaderActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopHeaderActions(false, true, false, {}, {}, {})
}

@Preview @Composable private fun ShopHeaderActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopHeaderActions(true, true, false, {}, {}, {})
}
