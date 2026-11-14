package com.coffeepeek.feature.shop.impl.ui.compose.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.feature.shop.api.ShopContentAction
import com.coffeepeek.feature.shop.api.ShopContentMetric
import com.coffeepeek.feature.shop.api.ShopExposure
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
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_missing
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopOverviewHero(
    overview: ShopOverview,
    onOpenPhoto: (urls: List<String>, originals: List<String>, index: Int) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photos = overview.photos
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        (if (photos.isNotEmpty()) modifier.shopExposure(ShopExposure.Photos) else modifier).fillMaxWidth()
            .heightIn(min = 300.dp + topInset)
            .clip(RoundedCornerShape(bottomStart = CoffeePeekDimens.radiusLg,
                bottomEnd = CoffeePeekDimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ShopOverviewPhotoPager(overview, pagerState, onOpenPhoto, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.2f),
            0.38f to Color.Transparent,
            0.74f to CoffeePeekColors.PhotoBadgeOverlay,
            1f to Color.Black.copy(alpha = 0.8f),
        )))
        ShopOverviewHeader(overview, onOpenMap, Modifier.align(Alignment.BottomStart).padding(
            start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing4,
            top = topInset + CoffeePeekDimens.glassControlSize + CoffeePeekDimens.spacing4,
            bottom = CoffeePeekDimens.spacing3,
        ), currentPage = pagerState.currentPage)
    }
}

@Composable
private fun ShopOverviewPhotoPager(
    overview: ShopOverview, pagerState: PagerState,
    onOpenPhoto: (List<String>, List<String>, Int) -> Unit, modifier: Modifier,
) {
    val photos = overview.photos
    if (photos.isEmpty()) {
        CoffeePeekImagePlaceholder(modifier, labelSize = 24.sp,
            contentDescription = stringResource(Res.string.shop_overview_photo_missing))
        return
    }
    val analytics = LocalShopAnalytics.current
    HorizontalPager(state = pagerState, modifier = modifier) { index ->
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

@Preview @Composable private fun ShopOverviewHeroLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewHero(previewOverview(), { _, _, _ -> }, {})
}

@Preview @Composable private fun ShopOverviewHeroDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewHero(previewOverview(), { _, _, _ -> }, {})
}

private fun previewOverview() = ShopOverview(
    id = "preview", title = "Кофейня", description = null,
    address = "Минск, улица Ленина, 1", latitude = 53.9, longitude = 27.5,
    rating = 4.6, reviewCount = 12, isOpen = true, photos = emptyList(),
)
