package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_photo_unavailable
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopPhotoTile(
    previewUrl: String?,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val url = previewUrl?.takeIf(String::isNotBlank)
    Box(modifier.size(88.dp).clickable(enabled = url != null, onClick = onClick),
        contentAlignment = Alignment.Center) {
        if (url == null) {
            PhotoUnavailable()
        } else {
            KamelImage(
                resource = { asyncPainterResource(url) },
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp),
                onLoading = { PhotoUnavailable() },
                onFailure = { PhotoUnavailable() },
            )
        }
    }
}

@Composable
private fun PhotoUnavailable() {
    Text(stringResource(Res.string.shop_photo_unavailable),
        style = MaterialTheme.typography.labelSmall)
}

@Preview @Composable private fun ShopPhotoTileLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopPhotoTile(null, "Фото кофейни", onClick = {})
}

@Preview @Composable private fun ShopPhotoTileDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopPhotoTile(null, "Фото кофейни", onClick = {})
}
