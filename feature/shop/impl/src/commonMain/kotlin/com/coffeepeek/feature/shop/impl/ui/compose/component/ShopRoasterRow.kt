package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopRoaster
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_open_roaster
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_roaster_photo_missing
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopRoasterRow(roaster: ShopRoaster, onOpenRoaster: (String) -> Unit, modifier: Modifier = Modifier) {
    val canOpen = roaster.id.isNotBlank()
    val openLabel = stringResource(Res.string.shop_coffee_open_roaster, roaster.name)
    Row(modifier.fillMaxWidth().then(if (canOpen) Modifier.clickable(role = Role.Button,
        onClickLabel = openLabel) { onOpenRoaster(roaster.id) } else Modifier)
        .padding(CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing5)) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val url = roaster.photoUrl
            if (url.isNullOrBlank()) RoasterPlaceholder()
            else KamelImage(resource = { asyncPainterResource(url) }, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onLoading = { RoasterPlaceholder() }, onFailure = { RoasterPlaceholder() })
        }
        Text(roaster.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (canOpen) Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoasterPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(CoffeePeekIcons.Coffee, stringResource(Res.string.shop_coffee_roaster_photo_missing), Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun ShopRoasterRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopRoasterRow(ShopRoaster("roaster", "Местная обжарка", null), {})
}
@Preview @Composable private fun ShopRoasterRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopRoasterRow(ShopRoaster("roaster", "Местная обжарка", null), {})
}
