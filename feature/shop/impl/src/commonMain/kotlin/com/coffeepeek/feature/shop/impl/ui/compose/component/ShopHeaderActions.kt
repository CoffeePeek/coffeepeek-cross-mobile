package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import dev.chrisbanes.haze.HazeState
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
    hazeState: HazeState? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        CoffeePeekGlassIconButton(onClick = onSuggestChange,
            contentDescription = stringResource(Res.string.shop_detail_suggest_change), hazeState = hazeState) {
            Icon(CoffeePeekIcons.NoteEdit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        ShopFavoriteButton(isFavorite, favoriteAvailable, isFavoriteLoading, onToggleFavorite,
            hazeState = hazeState)
        CoffeePeekGlassIconButton(onClick = onShare,
            contentDescription = stringResource(Res.string.shop_detail_share),
            modifier = Modifier.shopExposure(ShopExposure.Share), hazeState = hazeState) {
            Icon(CoffeePeekIcons.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview @Composable private fun ShopHeaderActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopHeaderActions(false, true, false, {}, {}, {})
}

@Preview @Composable private fun ShopHeaderActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopHeaderActions(true, true, false, {}, {}, {})
}
