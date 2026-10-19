package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.domain.model.RoasterPhoto
import com.coffeepeek.feature.roaster.impl.R
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoasterDetailHero(
    name: String,
    photos: List<RoasterPhoto>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    image: RoasterImage,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(280.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (photos.isEmpty()) {
            CoffeePeekImagePlaceholder(labelSize = 24.sp, contentDescription = stringResource(R.string.roaster_photos_missing, name))
        } else {
            val pager = rememberPagerState(pageCount = { photos.size })
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                val photo = photos[page]
                image(photo.url, name, Modifier.fillMaxSize().clickable { onPhotoClick(photo.url) }, ContentScale.Crop, 24.sp)
            }
            if (photos.size > 1) Surface(Modifier.align(Alignment.BottomEnd).padding(CoffeePeekDimens.spacing3),
                shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                Text(stringResource(R.string.roaster_photo_position, pager.currentPage + 1, photos.size), color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing1))
            }
        }
        CoffeePeekGlassIconButton(onBack, stringResource(R.string.roaster_detail_back),
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(CoffeePeekDimens.spacing2)) {
            Icon(CoffeePeekIcons.Back, null)
        }
        RoasterFavoriteHeart(isFavorite, onToggleFavorite, overImage = true,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(CoffeePeekDimens.spacing3))
    }
}

@PreviewLightDark @Composable private fun RoasterDetailHeroPreview() = CoffeePeekTheme {
    RoasterDetailHero("Sandbox Roaster", listOf(RoasterPhoto("1", "https://x/1", 1), RoasterPhoto("2", "https://x/2", 2)),
        isFavorite = true, {}, {}, {}, { _, _, _, _, _ -> })
}

@PreviewLightDark @Composable private fun RoasterDetailHeroEmptyPreview() = CoffeePeekTheme {
    RoasterDetailHero("Sandbox Roaster", emptyList(), isFavorite = false, {}, {}, {}, { _, _, _, _, _ -> })
}
