package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButtonStyle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_favorite_add
import com.coffeepeek.feature.roaster.impl.resources.roaster_favorite_remove

/** The shared affinity heart. Haptic feedback is an application concern passed through [onClick]. */
@Composable
internal fun RoasterFavoriteHeart(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overImage: Boolean = false,
) {
    CoffeePeekLikeButton(
        selected = isFavorite,
        contentDescription = stringResource(if (isFavorite) Res.string.roaster_favorite_remove else Res.string.roaster_favorite_add),
        onClick = onClick,
        enabled = enabled,
        overImage = overImage,
        style = if (overImage) CoffeePeekLikeButtonStyle.FloatingGlass else CoffeePeekLikeButtonStyle.Plain,
        modifier = modifier.size(48.dp),
    )
}

@Preview @Composable private fun RoasterFavoriteHeartLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterFavoriteHeart(isFavorite = false, onClick = {}, overImage = true)
}

@Preview @Composable private fun RoasterFavoriteHeartDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterFavoriteHeart(isFavorite = true, onClick = {}, overImage = true)
}
