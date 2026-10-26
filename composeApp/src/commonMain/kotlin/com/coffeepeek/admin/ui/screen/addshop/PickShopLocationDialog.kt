package com.coffeepeek.admin.ui.screen.addshop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformMapControlButton
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.PlatformFullScreenDialog
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekPlatformCoffeeIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun PickShopLocationDialog(
    initialPoint: GeoPoint?,
    onConfirm: (latitude: Double, longitude: Double, address: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var cameraCenter by remember(initialPoint) {
        mutableStateOf(initialPoint)
    }
    var cameraTarget by remember(initialPoint) {
        mutableStateOf(initialPoint?.let { it.latitude to it.longitude })
    }
    var myLocationRequestKey by remember { mutableIntStateOf(0) }
    var previewAddress by remember { mutableStateOf<String?>(null) }
    var resolvedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val onConfirmState = rememberUpdatedState(onConfirm)
    val hasCurrentAddress = cameraCenter != null && resolvedPoint == cameraCenter &&
        !previewAddress.isNullOrBlank()

    fun updateCameraCenter(point: GeoPoint) {
        val current = cameraCenter
        // Native map layout can report insignificant floating-point drift. Keeping
        // sub-meter changes stable avoids re-geocoding an unchanged selection.
        if (current == null || abs(current.latitude - point.latitude) > 0.000001 ||
            abs(current.longitude - point.longitude) > 0.000001
        ) {
            cameraCenter = point
        }
    }

    LaunchedEffect(cameraCenter) {
        val point = cameraCenter ?: return@LaunchedEffect
        isResolvingAddress = true
        resolveError = null
        delay(350)
        val address = PlatformLocation.reverseGeocode(point.latitude, point.longitude)
        previewAddress = address
        resolvedPoint = point
        isResolvingAddress = false
        if (address.isNullOrBlank()) {
            resolveError = "Не удалось определить адрес. Подвиньте карту."
        }
    }

    PlatformFullScreenDialog(onDismiss = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            val compactHeight = maxHeight < 480.dp && maxWidth > maxHeight
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            horizontal = CpDimens.spacing5,
                            vertical = if (compactHeight) CpDimens.spacing2 else CpDimens.spacing3,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Точка на карте",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        if (!compactHeight) {
                            Spacer(Modifier.height(CpDimens.spacing1))
                            Text(
                                text = "Переместите карту к кофейне",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    PlatformGlassIconButton(
                        icon = GlassControlIcon.Close,
                        onClick = onDismiss,
                        contentDescription = "Отменить выбор точки",
                    ) {
                        Icon(CpIcons.Close, contentDescription = "Отменить выбор точки")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds(),
                ) {
                    CoffeeMap(
                        shops = emptyList(),
                        selectedShopId = null,
                        onBoundsChanged = { _, _ -> },
                        onShopClick = {},
                        modifier = Modifier.fillMaxSize(),
                        cameraTarget = cameraTarget,
                        cameraZoom = 16f,
                        onCameraTargetApplied = { cameraTarget = null },
                        isDarkTheme = isDarkTheme,
                        myLocationRequestKey = myLocationRequestKey,
                        onMyLocationFound = { lat, lon -> updateCameraCenter(GeoPoint(lat, lon)) },
                        onCameraCenterChanged = { lat, lon -> updateCameraCenter(GeoPoint(lat, lon)) },
                    )

                    ShopLocationPin(modifier = Modifier.align(Alignment.Center))

                    PlatformMapControlButton(
                        icon = GlassControlIcon.Location,
                        onClick = { myLocationRequestKey += 1 },
                        contentDescription = "Моё местоположение",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(CpDimens.spacing4),
                    ) {
                        Icon(CpIcons.Navigation, contentDescription = "Моё местоположение")
                    }
                }

                ShopLocationConfirmationPanel(
                    addressText = when {
                        isResolvingAddress -> "Определяем адрес…"
                        hasCurrentAddress -> previewAddress.orEmpty()
                        else -> resolveError ?: "Переместите карту, чтобы выбрать точку"
                    },
                    isResolvingAddress = isResolvingAddress,
                    hasCurrentAddress = hasCurrentAddress,
                    compactHeight = compactHeight,
                    onConfirm = {
                        val point = cameraCenter ?: return@ShopLocationConfirmationPanel
                        if (resolvedPoint != point) return@ShopLocationConfirmationPanel
                        val address = previewAddress?.trim().orEmpty()
                        if (address.isBlank()) return@ShopLocationConfirmationPanel
                        onConfirmState.value(point.latitude, point.longitude, address)
                    },
                )
            }
        }
    }
}

@Composable
private fun ShopLocationConfirmationPanel(
    addressText: String,
    isResolvingAddress: Boolean,
    hasCurrentAddress: Boolean,
    compactHeight: Boolean,
    onConfirm: () -> Unit,
) {
    // Reserve the same two address lines for loading, errors and the result. Changing
    // this height also resizes the native map and can trigger a new camera callback.
    val address: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier
                .heightIn(min = CpDimens.controlHeight)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            if (isResolvingAddress) {
                CoffeePeekLoader(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    CpIcons.Location,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (!compactHeight) {
                    Text(
                        text = "Адрес кофейни",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(CpDimens.spacing1))
                }
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasCurrentAddress) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    val confirm: @Composable (Modifier) -> Unit = { modifier ->
        AppButton(
            text = if (compactHeight) "Выбрать" else "Выбрать эту точку",
            onClick = onConfirm,
            enabled = !isResolvingAddress && hasCurrentAddress,
            modifier = modifier.testTag("shop_location_confirm"),
        )
    }

    // This panel stays outside the viewport, keeping map attribution unobscured.
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (compactHeight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = CpDimens.spacing5, vertical = CpDimens.spacing2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                ) {
                    address(Modifier.weight(1f))
                    confirm(Modifier.width(144.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(CpDimens.spacing5),
                ) {
                    address(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(CpDimens.spacing4))
                    confirm(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private val ShopLocationPinShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    moveTo(width / 2, height)
    cubicTo(width * 0.36f, height * 0.78f, 0f, height * 0.55f, 0f, height * 0.39f)
    cubicTo(0f, height * 0.17f, width * 0.22f, 0f, width / 2, 0f)
    cubicTo(width * 0.78f, 0f, width, height * 0.17f, width, height * 0.39f)
    cubicTo(width, height * 0.55f, width * 0.64f, height * 0.78f, width / 2, height)
    close()
}

@Composable
private fun ShopLocationPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            // Anchor the tip, rather than the icon's center, to the selected coordinate.
            .offset(y = (-28).dp)
            .size(width = 44.dp, height = 56.dp)
            .shadow(6.dp, ShopLocationPinShape)
            .background(MaterialTheme.colorScheme.primary, ShopLocationPinShape)
            .semantics { contentDescription = "Метка расположения кофейни" }
            .testTag("shop_location_pin"),
        contentAlignment = Alignment.TopCenter,
    ) {
        CoffeePeekPlatformCoffeeIcon(
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(top = 9.dp).size(24.dp),
        )
    }
}

@Composable
private fun ShopLocationPinPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { ShopLocationPin() }
    }
}

@Preview @Composable
private fun ShopLocationPinLightPreview() = ShopLocationPinPreview(false)

@Preview @Composable
private fun ShopLocationPinDarkPreview() = ShopLocationPinPreview(true)
