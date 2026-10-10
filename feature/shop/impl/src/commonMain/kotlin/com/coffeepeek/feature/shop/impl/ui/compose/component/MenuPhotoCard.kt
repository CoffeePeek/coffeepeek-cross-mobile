package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_photo_counter
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_photo_unavailable
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MenuPhotoCard(
    photo: MenuPhoto,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.shop_menu_gallery_photo_description, index + 1, count)
    Box(
        modifier.fillMaxWidth().aspectRatio(0.72f)
            .clip(RoundedCornerShape(CpDimens.radiusLg)).clickable(onClick = onClick),
    ) {
        if (photo.previewUrl.isBlank()) {
            PhotoUnavailable(Modifier.fillMaxSize())
        } else {
            KamelImage(
                resource = { asyncPainterResource(photo.previewUrl) },
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onLoading = { PhotoUnavailable(Modifier.fillMaxSize()) },
                onFailure = { PhotoUnavailable(Modifier.fillMaxSize()) },
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.48f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(CpDimens.spacing3),
        ) {
            Text(
                stringResource(Res.string.shop_menu_gallery_photo_counter, index + 1, count),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing1),
            )
        }
    }
}

@Composable
private fun PhotoUnavailable(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.shop_menu_gallery_photo_unavailable),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun MenuPhotoCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    MenuPhotoCard(MenuPhoto("preview", ""), 0, 3, {})
}

@Preview @Composable private fun MenuPhotoCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    MenuPhotoCard(MenuPhoto("preview", ""), 1, 3, {})
}
