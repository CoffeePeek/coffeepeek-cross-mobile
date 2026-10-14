package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_back
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Screen-owned overlay; it never registers a backdrop source or owns navigation. */
@Composable
internal fun ShopDetailTopActions(
    showShopActions: Boolean,
    isFavorite: Boolean,
    favoriteAvailable: Boolean,
    isFavoriteLoading: Boolean,
    onBack: () -> Unit,
    onSuggestChange: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val backScale = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(CoffeePeekDimens.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        CoffeePeekGlassIconButton(onClick = onBack,
            contentDescription = stringResource(Res.string.shop_detail_back), hazeState = hazeState) {
            Icon(CoffeePeekIcons.Back, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.scale(scaleX = backScale, scaleY = 1f))
        }
        if (showShopActions) {
            ShopHeaderActions(isFavorite, favoriteAvailable, isFavoriteLoading,
                onSuggestChange, onToggleFavorite, onShare, hazeState)
        }
    }
}

@Preview @Composable private fun ShopDetailTopActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailTopActions(true, false, true, false, {}, {}, {}, {})
}

@Preview @Composable private fun ShopDetailTopActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailTopActions(true, true, true, false, {}, {}, {}, {})
}
