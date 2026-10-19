package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_count
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_add
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_remove
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_like_updating
import org.jetbrains.compose.resources.stringResource
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
    Row(modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (onClick != null) CoffeePeekLikeButton(isHelpful, description, onClick,
            isLoading = isLoading, loadingDescription = description)
        else Box(Modifier.size(48.dp).semantics {
            contentDescription = description; selected = isHelpful
        }, contentAlignment = Alignment.Center) {
            if (isLoading) CoffeePeekLoader(description, size = 24.dp)
            else CoffeePeekLikeIcon(isHelpful, contentDescription = null)
        }
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview @Composable private fun ShopCheckInHelpfulLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInHelpfulButton(3, false, {})
}
@Preview @Composable private fun ShopCheckInHelpfulDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInHelpfulButton(4, true, {})
}
@Preview @Composable private fun ShopCheckInHelpfulLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInHelpfulButton(3, false, null, isLoading = true)
}
@Preview @Composable private fun ShopCheckInHelpfulLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInHelpfulButton(4, true, null, isLoading = true)
}
