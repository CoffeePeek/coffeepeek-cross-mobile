package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekRoasterCard
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_catalog
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_logo
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_remove
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_shops
import com.coffeepeek.feature.favorites.impl.ui.compose.favoriteExposure
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The design-system roaster card with this feature's texts, logo loading and the remove-from-favorites heart. */
@Composable
internal fun FavoriteRoasterCard(
    roaster: FavoriteRoaster,
    removing: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val photo = roaster.photoUrl?.takeIf(String::isNotBlank)
    CoffeePeekRoasterCard(
        name = roaster.title,
        description = roaster.description,
        shopsCount = roaster.coffeeShopsCount,
        shopsLabel = stringResource(Res.string.favorites_roaster_shops),
        productsCount = roaster.coffeeProductsCount,
        productsLabel = stringResource(Res.string.favorites_roaster_catalog),
        hasLogo = photo != null,
        onClick = onOpen.takeIf { !roaster.publicSlug.isNullOrBlank() },
        logo = {
            if (photo != null) KamelImage(
                resource = { asyncPainterResource(photo) },
                contentDescription = stringResource(Res.string.favorites_roaster_logo, roaster.title),
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                onLoading = { RoasterPlaceholder() }, onFailure = { RoasterPlaceholder() },
            )
        },
        favorite = {
            CoffeePeekLikeButton(true, stringResource(Res.string.favorites_roaster_remove, roaster.title), onRemove,
                enabled = !removing && roaster.id.isNotBlank(), modifier = Modifier.favoriteExposure(roaster = true).size(48.dp))
        },
    )
}

@Composable private fun RoasterPlaceholder() {
    Icon(CoffeePeekIcons.Factory, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview @Composable private fun FavoriteRoasterCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoriteRoasterCard(FavoriteRoaster("sample", "Обжарщик", "sample", coffeeShopsCount = 7, coffeeProductsCount = 12,
        description = "Обжариваем светлую арабику для фильтра и эспрессо небольшими партиями"), false, {}, {})
}
@Preview @Composable private fun FavoriteRoasterCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoriteRoasterCard(FavoriteRoaster("sample", "Обжарщик с длинным названием без публичного адреса"), true, {}, {})
}
