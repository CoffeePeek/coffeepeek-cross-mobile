package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import androidx.compose.ui.unit.sp
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionPlaceRow(place: CollectionPlace, checked: Boolean?, enabled: Boolean,
    first: Boolean = true, last: Boolean = true, modifier: Modifier = Modifier,
    moveActions: List<CustomAccessibilityAction> = emptyList(), trailing: @Composable () -> Unit = {}, onClick: () -> Unit) {
    val interaction = if (checked == null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        else Modifier.toggleable(checked, enabled, role = Role.Checkbox, onValueChange = { onClick() })
    val shape = RoundedCornerShape(topStart = if (first) 16.dp else 0.dp, topEnd = if (first) 16.dp else 0.dp,
        bottomStart = if (last) 16.dp else 0.dp, bottomEnd = if (last) 16.dp else 0.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).then(interaction).semantics(mergeDescendants = true) { customActions = moveActions }
            .padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CollectionPlacePhoto(place, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { CollectionPlaceLabels(place, checked == null) }
            if (checked != null) Box(Modifier.size(24.dp)) {
                if (checked) CoffeePeekIcon(CoffeePeekIcons.Check, null, tint = MaterialTheme.colorScheme.coffeePeekAccentForeground)
            }
            else CoffeePeekIcon(CoffeePeekIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            trailing()
        }
        if (!last) HorizontalDivider(Modifier.padding(start = 76.dp, end = 16.dp), thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun CollectionPlaceLabels(place: CollectionPlace, showKind: Boolean) {
    Text(place.title, style = MaterialTheme.typography.titleMedium)
    val address = listOfNotNull(place.city, place.address).filter(String::isNotBlank).joinToString(" · ")
    val detail = address.ifBlank { if (!showKind) "" else stringResource(
        if (place.kind == CollectionPlaceKind.CoffeeShop) Res.string.collections_shop else Res.string.collections_roaster) }
    if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        address = "Кофейная улица, 1"), true, true, onClick = {})
}
@Preview @Composable private fun CollectionPlaceRowLightPreview() = CollectionPlaceRowPreview(false)
@Preview @Composable private fun CollectionPlaceRowDarkPreview() = CollectionPlaceRowPreview(true)
