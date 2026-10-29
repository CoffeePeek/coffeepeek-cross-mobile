package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.feature.profile.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.round

/** A viewed author's favorites are display-only markers, never the viewer's membership. */
@Composable
internal fun PublicProfileFavoriteCard(
    favorite: PublicProfileFavorite, image: ProfileImage, logoImage: ProfileImage,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val open = onClick.takeIf { !favorite.publicAddress?.slug.isNullOrBlank() }
    when (favorite.kind) {
        PublicFavoriteKind.CoffeeShop -> ProfileShopFavorite(favorite, image, modifier, open)
        PublicFavoriteKind.Roaster -> ProfileRoasterFavorite(favorite, logoImage, modifier, open)
        PublicFavoriteKind.Drink -> ProfileDrinkFavorite(favorite, image, modifier)
    }
}

@Composable
private fun ProfileShopFavorite(item: PublicProfileFavorite, image: ProfileImage, modifier: Modifier, onClick: (() -> Unit)?) {
    CoffeePeekShopCard(item.title, stringResource(Res.string.profile_favorite_shop), modifier,
        rating = item.rating, ratingText = item.rating?.let(::profileRatingText).orEmpty(), ratingCount = item.reviewCount,
        statusLabel = item.isOpen?.let { stringResource(if (it) Res.string.profile_favorite_open else Res.string.profile_favorite_closed) },
        isOpen = item.isOpen == true, locationSummary = item.address, onClick = onClick,
        cover = { ProfileFavoriteImage(item, image, Modifier.fillMaxSize()) },
        favorite = { CoffeePeekLikeBadge(true, stringResource(Res.string.profile_favorite_saved), Modifier.size(48.dp)) })
}

@Composable
private fun ProfileRoasterFavorite(item: PublicProfileFavorite, image: ProfileImage, modifier: Modifier, onClick: (() -> Unit)?) {
    CoffeePeekRoasterCard(item.title, item.description, item.coffeeShopsCount ?: 0,
        stringResource(Res.string.profile_favorite_shops), item.coffeeProductsCount ?: 0,
        stringResource(Res.string.profile_favorite_catalog), modifier,
        hasLogo = !item.imageUrl.isNullOrBlank(), onClick = onClick,
        logo = { item.imageUrl?.let { image(it, item.title, Modifier.fillMaxSize()) } },
        favorite = {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CoffeePeekLikeIcon(true, stringResource(Res.string.profile_favorite_saved))
            }
        })
}

@Composable
private fun ProfileDrinkFavorite(item: PublicProfileFavorite, image: ProfileImage, modifier: Modifier) {
    CoffeePeekCard(modifier, shape = RoundedCornerShape(CoffeePeekDimens.radiusXl), elevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        header = {
            Box(Modifier.fillMaxWidth().aspectRatio(2f).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                if (!item.imageUrl.isNullOrBlank()) image(item.imageUrl!!, item.title, Modifier.fillMaxSize())
                else Icon(CoffeePeekIcons.Coffee, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                CoffeePeekLikeBadge(true, stringResource(Res.string.profile_favorite_saved),
                    Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing3).size(40.dp))
            }
        }) {
        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        Text(stringResource(Res.string.profile_favorite_drink), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileFavoriteImage(item: PublicProfileFavorite, image: ProfileImage, modifier: Modifier) {
    val url = item.imageUrl?.takeIf(String::isNotBlank)
    if (url != null) image(url, item.title, modifier)
    else CoffeePeekImagePlaceholder(modifier, contentDescription = stringResource(Res.string.profile_favorite_no_photo, item.title))
}

private fun profileRatingText(value: Double): String {
    if (!value.isFinite()) return ""
    val tenths = round(value * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

@Composable
private fun ProfileFavoritePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PublicFavoriteKind.entries.forEach { kind ->
            PublicProfileFavoriteCard(PublicProfileFavorite("demo", "CoffeePeek", kind), { _, _, _ -> }, { _, _, _ -> }) {}
        }
    }
}

@Preview @Composable private fun ProfileFavoriteLightPreview() = ProfileFavoritePreview(false)
@Preview @Composable private fun ProfileFavoriteDarkPreview() = ProfileFavoritePreview(true)
