package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.domain.model.RoasterPhoto
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_photo_position
import com.coffeepeek.feature.roaster.impl.resources.roaster_photos_missing
import com.coffeepeek.feature.roaster.api.RoasterImage

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
    heartModifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()
        .height(280.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        .background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (photos.isEmpty()) {
            CoffeePeekImagePlaceholder(labelSize = 24.sp, contentDescription = stringResource(Res.string.roaster_photos_missing, name))
        } else {
            val pager = rememberPagerState(pageCount = { photos.size })
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                val photo = photos[page]
                image(photo.url, name, Modifier.fillMaxSize().clickable { onPhotoClick(photo.url) }, ContentScale.Crop, 24.sp)
            }
            if (photos.size > 1) Surface(Modifier.align(Alignment.BottomEnd).padding(CoffeePeekDimens.spacing3),
                shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                Text(stringResource(Res.string.roaster_photo_position, pager.currentPage + 1, photos.size), color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing1))
            }
        }
        RoasterDetailTopActions(isFavorite, onToggleFavorite, onBack,
            modifier = Modifier.align(Alignment.TopCenter), heartModifier = heartModifier)
    }
}

@Preview @Composable private fun RoasterDetailHeroLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailHero("Sandbox Roaster", listOf(RoasterPhoto("1", "https://x/1", 1), RoasterPhoto("2", "https://x/2", 2)),
        isFavorite = true, {}, {}, {}, { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterDetailHeroDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailHero("Sandbox Roaster", listOf(RoasterPhoto("1", "https://x/1", 1), RoasterPhoto("2", "https://x/2", 2)),
        isFavorite = true, {}, {}, {}, { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterDetailHeroEmptyLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailHero("Sandbox Roaster", emptyList(), isFavorite = false, {}, {}, {}, { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterDetailHeroEmptyDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailHero("Sandbox Roaster", emptyList(), isFavorite = false, {}, {}, {}, { _, _, _, _, _ -> })
}
