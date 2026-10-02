package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_existing_photos
import com.coffeepeek.feature.shop.impl.resources.shop_review_existing_photo_description
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReviewExistingPhotos(
    photoUrls: List<String>,
    onOpenPhoto: (List<String>, Int) -> Unit,
) {
    if (photoUrls.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Text(stringResource(Res.string.shop_review_existing_photos),
            style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            photoUrls.forEachIndexed { index, url ->
                ShopPhotoTile(url,
                    description = stringResource(Res.string.shop_review_existing_photo_description,
                        index + 1, photoUrls.size),
                    onClick = { onOpenPhoto(photoUrls, index) })
            }
        }
    }
}

@Preview @Composable private fun ShopReviewExistingPhotosLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewExistingPhotos(listOf(""), onOpenPhoto = { _, _ -> })
}

@Preview @Composable private fun ShopReviewExistingPhotosDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewExistingPhotos(listOf(""), onOpenPhoto = { _, _ -> })
}
