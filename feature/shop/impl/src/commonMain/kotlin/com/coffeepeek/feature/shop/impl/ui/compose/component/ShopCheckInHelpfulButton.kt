package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
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
    Row(modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
        .clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .then(if (onClick != null && !isLoading) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .semantics(mergeDescendants = true) { contentDescription = description; selected = isHelpful },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (isLoading) CoffeePeekLoader(description, size = 24.dp)
        else Icon(if (isHelpful) CoffeePeekIcons.FavoriteFilled else CoffeePeekIcons.Favorite,
            contentDescription = null, tint = CoffeePeekColors.Error, modifier = Modifier.size(24.dp))
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
