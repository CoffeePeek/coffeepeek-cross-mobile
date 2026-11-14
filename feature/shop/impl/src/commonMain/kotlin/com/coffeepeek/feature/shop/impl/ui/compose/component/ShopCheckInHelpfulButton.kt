package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeCount
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_count
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_add
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_remove
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_updating
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInHelpfulButton(count: Int, isHelpful: Boolean, onClick: (() -> Unit)?,
    isLoading: Boolean = false, modifier: Modifier = Modifier) {
    val description = stringResource(when {
        isLoading -> Res.string.shop_checkins_like_updating
        onClick == null -> Res.string.shop_checkins_like_count
        isHelpful -> Res.string.shop_checkins_like_remove
        else -> Res.string.shop_checkins_like_add
    }, count)
    CoffeePeekLikeCount(count, isHelpful, description, modifier, onClick, enabled = !isLoading)
}

@Preview @Composable private fun ShopCheckInHelpfulLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInHelpfulButton(3, false, {})
}
@Preview @Composable private fun ShopCheckInHelpfulDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInHelpfulButton(4, true, {})
}
@Preview @Composable private fun ShopCheckInHelpfulLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInHelpfulButton(4, true, null, isLoading = true)
}
@Preview @Composable private fun ShopCheckInHelpfulLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInHelpfulButton(4, true, null, isLoading = true)
}
