package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun FavoritesTopBar(canShare: Boolean, onBack: () -> Unit, onShare: () -> Unit) {
    CoffeePeekTopBar(stringResource(Res.string.favorites_title), stringResource(Res.string.favorites_back), onBack = onBack, actions = {
        CoffeePeekPlainIconButton(onShare, enabled = canShare) {
            Icon(CoffeePeekIcons.Share, stringResource(Res.string.favorites_share))
        }
    })
}

@Preview @Composable private fun FavoritesTopBarLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoritesTopBar(true, {}, {})
}
@Preview @Composable private fun FavoritesTopBarDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoritesTopBar(true, {}, {})
}
