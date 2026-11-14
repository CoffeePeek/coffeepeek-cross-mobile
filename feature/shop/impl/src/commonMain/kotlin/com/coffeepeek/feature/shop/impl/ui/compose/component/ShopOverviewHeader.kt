package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopPhoto
import com.coffeepeek.feature.shop.domain.model.ShopType
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_overview_photo_counter
import com.coffeepeek.feature.shop.impl.resources.shop_overview_show_on_map
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopOverviewHeader(overview: ShopOverview, onOpenMap: () -> Unit,
    modifier: Modifier = Modifier, currentPage: Int = 0) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < 296.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                ShopOverviewIdentity(overview, onOpenMap)
                ShopOverviewPhotoMetadata(overview.type, currentPage, overview.photos.size, Alignment.Start)
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                ShopOverviewIdentity(overview, onOpenMap, Modifier.weight(1f))
                ShopOverviewPhotoMetadata(overview.type, currentPage, overview.photos.size, Alignment.End)
            }
        }
    }
}

@Composable
private fun ShopOverviewPhotoMetadata(
    type: ShopType,
    currentPage: Int,
    photoCount: Int,
    alignment: Alignment.Horizontal,
) {
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        ShopTypeBadge(type)
        if (photoCount > 0) {
            Surface(shape = CircleShape, color = CoffeePeekColors.PhotoBadgeOverlay, contentColor = Color.White) {
                Text(stringResource(Res.string.shop_overview_photo_counter, currentPage.coerceAtMost(photoCount - 1) + 1, photoCount),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = CoffeePeekDimens.spacing1),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ShopOverviewIdentity(overview: ShopOverview, onOpenMap: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(overview.title, style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
            color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val canOpenMap = overview.latitude != null && overview.longitude != null
        val address = overview.address?.takeIf(String::isNotBlank)
            ?: if (canOpenMap) stringResource(Res.string.shop_overview_show_on_map) else null
        if (address != null) {
            val openMap = if (canOpenMap) Modifier.clickable(role = Role.Button, onClick = onOpenMap) else Modifier
            Row(openMap.heightIn(min = CoffeePeekDimens.minimumTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                CoffeePeekIcon(CoffeePeekIcons.Location, null, Modifier.size(16.dp), tint = Color.White)
                Text(address, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium,
                    color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ShopOverviewHeaderPreview(dark: Boolean, largeText: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 2f else density.fontScale)) {
        Surface(Modifier.width(if (largeText) 280.dp else 360.dp), color = CoffeePeekColors.DarkSurface) {
            ShopOverviewHeader(previewOverview().copy(photos = List(4) { ShopPhoto(it.toString(), "", "") }), {})
        }
    }
}

@Preview @Composable private fun ShopOverviewHeaderLightPreview() = ShopOverviewHeaderPreview(false)
@Preview @Composable private fun ShopOverviewHeaderDarkPreview() = ShopOverviewHeaderPreview(true)

@Preview @Composable private fun ShopOverviewHeaderLargeTextLightPreview() = ShopOverviewHeaderPreview(false, largeText = true)
@Preview @Composable private fun ShopOverviewHeaderLargeTextDarkPreview() = ShopOverviewHeaderPreview(true, largeText = true)

private fun previewOverview() = ShopOverview("preview", "Кофейня", null,
    "Минск, улица Ленина, 1", 53.9, 27.5, 4.6, 12, true, emptyList())
