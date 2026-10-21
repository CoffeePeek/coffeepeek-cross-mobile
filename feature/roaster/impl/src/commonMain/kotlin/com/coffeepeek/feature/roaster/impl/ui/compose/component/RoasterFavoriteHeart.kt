package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
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
        modifier = modifier.size(48.dp),
    )
}

@Preview @Composable private fun RoasterFavoriteHeartLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterFavoriteHeart(isFavorite = true, onClick = {})
}

@Preview @Composable private fun RoasterFavoriteHeartDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterFavoriteHeart(isFavorite = true, onClick = {})
}
