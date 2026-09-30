package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_counter
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_missing
import com.coffeepeek.feature.shop.impl.resources.shop_overview_show_on_map
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Stateless header; the application retains the photo viewer and map navigation. */
@Composable
internal fun ShopOverviewHero(
    overview: ShopOverview,
    onOpenPhoto: (urls: List<String>, index: Int) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photos = overview.photos
    val pagerState = rememberPagerState(pageCount = { photos.size })
    Box(
        modifier.fillMaxWidth().height(300.dp)
            .clip(RoundedCornerShape(bottomStart = CpDimens.radius3xl,
                bottomEnd = CpDimens.radius3xl))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (photos.isEmpty()) {
            PhotoPlaceholder(Modifier.fillMaxSize())
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                KamelImage(
                    resource = { asyncPainterResource(photos[index].previewUrl) },
                    contentDescription = stringResource(Res.string.shop_overview_photo_description, overview.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable {
                        onOpenPhoto(photos.map { it.fullUrl }, index)
                    },
                    onLoading = { PhotoPlaceholder(Modifier.fillMaxSize()) },
                    onFailure = { PhotoPlaceholder(Modifier.fillMaxSize()) },
                )
            }
        }

        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.72f),
        ))))

        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(start = CpDimens.spacing4, end = 116.dp, bottom = CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        ) {
            Text(
                overview.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val canOpenMap = overview.latitude != null && overview.longitude != null
            val address = overview.address ?: if (canOpenMap) {
                stringResource(Res.string.shop_overview_show_on_map)
            } else null
            address?.let { label ->
                Row(Modifier.clickable(enabled = canOpenMap, onClick = onOpenMap),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (photos.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(CpDimens.spacing4),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.48f),
            ) {
                Text(stringResource(Res.string.shop_overview_photo_counter,
                    pagerState.currentPage + 1, photos.size),
                    modifier = Modifier.padding(horizontal = CpDimens.spacing2,
                        vertical = CpDimens.spacing1),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.shop_overview_photo_missing),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun ShopOverviewHeroLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewHero(previewOverview(), { _, _ -> }, {})
}

@Preview @Composable private fun ShopOverviewHeroDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewHero(previewOverview(), { _, _ -> }, {})
}

private fun previewOverview() = ShopOverview(
    id = "preview", title = "Кофейня", description = null,
    address = "Минск, улица Ленина, 1", latitude = 53.9, longitude = 27.5,
    rating = 4.6, reviewCount = 12, isOpen = true, photos = emptyList(),
)
