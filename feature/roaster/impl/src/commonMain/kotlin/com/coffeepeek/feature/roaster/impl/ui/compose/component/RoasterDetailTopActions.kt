package com.coffeepeek.feature.roaster.impl.ui.compose.component

import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.icons.PlatformBack
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_detail_back
import com.coffeepeek.feature.roaster.impl.resources.roaster_share
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun RoasterDetailTopActions(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    heartModifier: Modifier = Modifier,
) {
    val backScale = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(CoffeePeekDimens.spacing3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        CoffeePeekGlassIconButton(onBack, stringResource(Res.string.roaster_detail_back), nativeIcon = CoffeePeekGlassIcon.Back) {
            Icon(CoffeePeekIcons.PlatformBack, null,
                modifier = Modifier.scale(scaleX = backScale, scaleY = 1f))
        }
        val business = LocalRoasterBusinessIdentity.current
        val ownership = com.coffeepeek.feature.ownership.api.LocalBusinessOwnership.current
        if (business != null && ownership != null) ownership.ProfileActions("Roaster", business.id, business.slug, {}, onOwnerEdit = {},
            favorite = { RoasterFavoriteHeart(isFavorite, onToggleFavorite, overImage = true, modifier = heartModifier) },
            share = {
                val share = LocalRoasterShare.current
                CoffeePeekGlassIconButton({ share(business.canonicalPath) }, stringResource(Res.string.roaster_share), nativeIcon = CoffeePeekGlassIcon.Share) {
                    Icon(CoffeePeekIcons.Share, null)
                }
            })
        else RoasterFavoriteHeart(isFavorite, onToggleFavorite, overImage = true, modifier = heartModifier)
    }
}

@Preview @Composable private fun RoasterDetailTopActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailTopActions(false, {}, {})
}

@Preview @Composable private fun RoasterDetailTopActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailTopActions(true, {}, {})
}
