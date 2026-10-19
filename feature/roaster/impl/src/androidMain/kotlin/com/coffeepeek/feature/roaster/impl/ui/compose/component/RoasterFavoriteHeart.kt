package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.R

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
        contentDescription = stringResource(if (isFavorite) R.string.roaster_favorite_remove else R.string.roaster_favorite_add),
        onClick = onClick,
        enabled = enabled,
        overImage = overImage,
        modifier = modifier.size(48.dp),
    )
}

@PreviewLightDark @Composable private fun RoasterFavoriteHeartPreview() = CoffeePeekTheme {
    RoasterFavoriteHeart(isFavorite = true, onClick = {})
}
