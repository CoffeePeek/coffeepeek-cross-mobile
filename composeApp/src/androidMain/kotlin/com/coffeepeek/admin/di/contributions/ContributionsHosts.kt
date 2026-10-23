package com.coffeepeek.admin.di.contributions

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.PriceBeanSlider
import com.coffeepeek.admin.ui.component.RoasterAvatar
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.ui.component.priceLevelValue
import com.coffeepeek.admin.ui.screen.addshop.PickShopLocationDialog
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.feature.contributions.api.AddRoasterHost
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.api.ChangeEditorHost
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.model.PriceRange
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.admin.location.GeoPoint as LegacyGeoPoint

/** The legacy pieces the migrated forms borrow until they have design-system versions: the photo picker, the map dialog, the price slider, the icons. */
private val photoSection: @androidx.compose.runtime.Composable (List<PendingPhoto>, Int, String, String, (List<PendingPhoto>) -> Unit, (Int) -> Unit) -> Unit =
    { photos, max, title, hint, onAdd, onRemove ->
        PhotoAttachmentsSection(
            photos = photos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
            maxPhotos = max,
            onPhotosAdded = { picked -> onAdd(picked.map { PendingPhoto(it.fileName, it.contentType, it.bytes) }) },
            onRemovePhoto = onRemove,
            title = title,
            hint = hint,
        )
    }

private val brewIcon: @androidx.compose.runtime.Composable (String) -> Unit = { name ->
    Icon(painterResource(brewMethodIcon(name)), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
}

private val roasterLogo: @androidx.compose.runtime.Composable (CatalogOption) -> Unit = { RoasterAvatar(CatalogItem(it.id, it.name, photoUrl = it.photoUrl)) }

private fun haptic(success: Boolean) = platformHaptic(if (success) HapticEvent.Success else HapticEvent.Error)

internal fun addShopHost() = AddShopHost(
    locationPermission = { onGranted, onDenied -> LocationPermissionEffect(requestKey = 1, onGranted = onGranted, onDenied = onDenied) },
    mapPicker = { initial, onConfirm, onDismiss ->
        PickShopLocationDialog(
            initialPoint = initial?.let { LegacyGeoPoint(it.latitude, it.longitude) },
            onConfirm = { latitude, longitude, address -> onConfirm(GeoPoint(latitude, longitude), address) },
            onDismiss = onDismiss,
        )
    },
    photos = photoSection,
    priceSelector = { selected, onSelect ->
        val level = selected?.let { it.ordinal + 1 }
        PriceBeanSlider(
            selected = level,
            onSelect = { onSelect(it?.let { value -> PriceRange.entries.getOrNull(value - 1) }) },
            title = priceLevelValue(level)?.let { "Цена (капучино за $it)" } ?: "Цена",
            showHint = false,
        )
    },
    brewMethodIcon = brewIcon,
    roasterLogo = roasterLogo,
    shopImage = { bytes, modifier ->
        if (bytes != null) CpImage(data = bytes, modifier = modifier, contentScale = ContentScale.Crop)
        else CoffeeShopPlaceholderImage(modifier, labelSize = 16.sp)
    },
    feedback = ::haptic,
)

internal fun addRoasterHost() = AddRoasterHost(photos = photoSection, feedback = ::haptic)

internal fun changeEditorHost() = ChangeEditorHost(
    photos = photoSection,
    remoteImage = { url, modifier -> CpImage(data = url, modifier = modifier, contentScale = ContentScale.Crop) },
    brewMethodIcon = brewIcon,
    roasterLogo = roasterLogo,
    feedback = ::haptic,
)
