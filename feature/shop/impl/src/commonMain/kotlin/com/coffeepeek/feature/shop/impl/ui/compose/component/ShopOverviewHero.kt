package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.feature.shop.api.*
import com.coffeepeek.feature.shop.impl.ui.data.LocalShopAnalytics
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_counter
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_missing
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Photo-only hero; title, type and address are rendered below, as in the current screen. */
@Composable
internal fun ShopOverviewHero(
    overview: ShopOverview,
    onOpenPhoto: (urls: List<String>, originals: List<String>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val analytics = LocalShopAnalytics.current
    val photos = overview.photos
    val pagerState = rememberPagerState(pageCount = { photos.size })
    Box(
        (if (photos.isNotEmpty()) modifier.shopExposure(ShopExposure.Photos) else modifier).fillMaxWidth()
            .height(300.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .clip(RoundedCornerShape(bottomStart = CoffeePeekDimens.radiusLg,
                bottomEnd = CoffeePeekDimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (photos.isEmpty()) {
            CoffeePeekImagePlaceholder(labelSize = 24.sp,
                contentDescription = stringResource(Res.string.shop_overview_photo_missing))
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                KamelImage(
                    resource = { asyncPainterResource(photos[index].previewUrl) },
                    contentDescription = stringResource(Res.string.shop_overview_photo_description, overview.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable {
                        analytics.content(ShopContentMetric.Photos, ShopContentAction.Open)
                        onOpenPhoto(photos.map { it.previewUrl }, photos.map { it.fullUrl }, index)
                    },
                    onLoading = { CoffeePeekImagePlaceholder(labelSize = 24.sp) },
                    onFailure = { CoffeePeekImagePlaceholder(labelSize = 24.sp) },
                )
            }
        }

        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.35f),
        ))))

        if (photos.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(CoffeePeekDimens.spacing3),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.6f),
            ) {
                Text(stringResource(Res.string.shop_overview_photo_counter,
                    pagerState.currentPage + 1, photos.size),
                    modifier = Modifier.padding(horizontal = 10.dp,
                        vertical = CoffeePeekDimens.spacing1),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview @Composable private fun ShopOverviewHeroLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewHero(previewOverview(), { _, _, _ -> })
}

@Preview @Composable private fun ShopOverviewHeroDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewHero(previewOverview(), { _, _, _ -> })
}

private fun previewOverview() = ShopOverview(
    id = "preview", title = "Кофейня", description = null,
    address = "Минск, улица Ленина, 1", latitude = 53.9, longitude = 27.5,
    rating = 4.6, reviewCount = 12, isOpen = true, photos = emptyList(),
)
