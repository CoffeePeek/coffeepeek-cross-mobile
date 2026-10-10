package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_review
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReviewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(CpIcons.Review, contentDescription = null)
        Text(stringResource(Res.string.shop_detail_review), Modifier.padding(start = CpDimens.spacing2))
    }
}

@Preview @Composable private fun ShopReviewButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewButton(onClick = {})
}

@Preview @Composable private fun ShopReviewButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewButton(onClick = {})
}
