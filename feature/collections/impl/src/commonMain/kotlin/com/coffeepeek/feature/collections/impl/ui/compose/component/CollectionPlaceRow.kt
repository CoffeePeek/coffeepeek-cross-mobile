package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import androidx.compose.ui.unit.sp
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionPlaceRow(place: CollectionPlace, checked: Boolean?, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surface).toggleable(checked ?: false, enabled,
            role = if (checked == null) Role.Button else Role.Checkbox, onValueChange = { onClick() })
        .padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CollectionPlacePhoto(place, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(place.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(listOfNotNull(place.city, place.address).filter(String::isNotBlank).joinToString(" · ").ifBlank {
                stringResource(if (place.kind == CollectionPlaceKind.CoffeeShop) Res.string.collections_shop else Res.string.collections_roaster)
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        checked?.let { Checkbox(it, onCheckedChange = null, enabled = enabled) }
    }
}

@Composable
private fun CollectionPlacePhoto(place: CollectionPlace, modifier: Modifier) {
    val photo = place.photoUrl
    if (photo == null) CollectionPhotoFallback(modifier)
    else KamelImage(resource = { asyncPainterResource(photo) }, contentDescription = null, modifier = modifier,
        contentScale = if (place.kind == CollectionPlaceKind.Roaster) ContentScale.Fit else ContentScale.Crop,
        onLoading = { CollectionPhotoFallback(Modifier.fillMaxSize()) },
        onFailure = { CollectionPhotoFallback(Modifier.fillMaxSize()) })
}

@Composable
private fun CollectionPhotoFallback(modifier: Modifier) {
    CoffeePeekImagePlaceholder(modifier, labelSize = 8.sp)
}

@Composable
private fun CollectionPlaceRowPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CollectionPlaceRow(CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня", city = "Минск",
        address = "Кофейная улица, 1"), true, true, {})
}
@Preview @Composable private fun CollectionPlaceRowLightPreview() = CollectionPlaceRowPreview(false)
@Preview @Composable private fun CollectionPlaceRowDarkPreview() = CollectionPlaceRowPreview(true)
