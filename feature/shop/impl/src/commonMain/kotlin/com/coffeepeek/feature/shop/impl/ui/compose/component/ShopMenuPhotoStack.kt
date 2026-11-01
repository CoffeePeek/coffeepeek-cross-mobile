package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_photo_description
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val MenuPhotoWidth = 124.dp
private val MenuPhotoHeight = 248.dp
private val MenuPhotoPeek = 12.dp
private const val MAX_PEEKS = 2

/** Photo tap opens the fullscreen album at this index; gallery navigation is a separate action. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShopMenuPhotoStack(
    photos: List<MenuPhoto>,
    onOpenPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    if (photos.isEmpty()) return
    val pager = rememberPagerState(pageCount = { photos.size })
    val peekPx = with(LocalDensity.current) { MenuPhotoPeek.toPx() }
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    HorizontalPager(state = pager, pageSize = PageSize.Fixed(MenuPhotoWidth),
        beyondViewportPageCount = MAX_PEEKS,
        modifier = modifier.width(MenuPhotoWidth + MenuPhotoPeek * (photos.size - 1).coerceAtMost(MAX_PEEKS))
            .height(MenuPhotoHeight)) { page ->
        val depth = (page - pager.currentPage - pager.currentPageOffsetFraction).coerceAtLeast(0f)
        val visibleDepth = depth.coerceAtMost(MAX_PEEKS.toFloat())
        val description = stringResource(Res.string.shop_menu_gallery_photo_description, page + 1, photos.size)
        Box(Modifier.zIndex(-depth).graphicsLayer {
            if (depth > 0f) {
                translationX = visibleDepth * peekPx - depth * size.width
                scaleX = 1f - 0.08f * visibleDepth
                scaleY = scaleX
                transformOrigin = TransformOrigin(1f, 0.5f)
                alpha = (MAX_PEEKS + 1 - depth).coerceIn(0f, 1f)
            }
        }.fillMaxSize().shadow(if (depth < 0.5f) 4.dp else 0.dp, shape).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(role = Role.Button, onClickLabel = description) { onOpenPhoto(page) }) {
            val url = photos[page].previewUrl
            if (url.isBlank()) MenuPhotoPlaceholder()
            else KamelImage(resource = { asyncPainterResource(url) }, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onLoading = { MenuPhotoPlaceholder() }, onFailure = { MenuPhotoPlaceholder() })
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
            overlay()
        }
    }
}

@Composable
private fun MenuPhotoPlaceholder() {
    CoffeePeekImagePlaceholder(labelSize = 14.sp)
}

@Preview @Composable private fun ShopMenuPhotoStackLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuPhotoStack(List(3) { MenuPhoto("preview-$it", "", "") }, {})
}
@Preview @Composable private fun ShopMenuPhotoStackDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuPhotoStack(List(3) { MenuPhoto("preview-$it", "", "") }, {})
}
