package com.coffeepeek.feature.contributions.impl.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.*
import com.coffeepeek.feature.contributions.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContributionMapPickerContent(state: ContributionMapUiState, canvas: ContributionMapCanvas,
    onCenter: (GeoPoint) -> Unit, onCameraApplied: () -> Unit, onLocation: () -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        val compact = maxHeight < 480.dp && maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            MapPickerHeader(compact, onDismiss)
            MapPickerViewport(state, canvas, onCenter, onCameraApplied, onLocation, Modifier.fillMaxWidth().weight(1f))
            ContributionMapConfirmation(state, compact, onConfirm)
        }
    }
}

@Composable
private fun MapPickerHeader(compact: Boolean, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding()
        .padding(horizontal = CoffeePeekDimens.spacing5, vertical = if (compact) CoffeePeekDimens.spacing2 else CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.contributions_map_title), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
            if (!compact) {
                Spacer(Modifier.height(CoffeePeekDimens.spacing1))
                Text(stringResource(Res.string.contributions_map_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val cancel = stringResource(Res.string.contributions_map_cancel)
        CoffeePeekGlassIconButton(onDismiss, cancel, nativeIcon = CoffeePeekGlassIcon.Close) { Icon(CoffeePeekIcons.Close, cancel) }
    }
}

@Composable
private fun MapPickerViewport(state: ContributionMapUiState, canvas: ContributionMapCanvas, onCenter: (GeoPoint) -> Unit,
    onCameraApplied: () -> Unit, onLocation: () -> Unit, modifier: Modifier) {
    Box(modifier.clipToBounds()) {
        canvas.Content(ContributionMapCanvasState(state.cameraTarget, state.locationRequest, MaterialTheme.colorScheme.background.luminance() < 0.5f),
            ContributionMapCallbacks(onCameraApplied, onCenter, onCenter), Modifier.fillMaxSize())
        ContributionLocationPin(Modifier.align(Alignment.Center))
        val location = stringResource(Res.string.contributions_map_my_location)
        CoffeePeekMapControlButton(CoffeePeekGlassIcon.Location, onLocation, location,
            Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing4)) { Icon(CoffeePeekIcons.Navigation, location) }
    }
}

@Composable private fun MapPickerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ContributionMapPickerContent(ContributionMapUiState(point = GeoPoint(53.9, 27.5), address = "Тестовый адрес", resolvedPoint = GeoPoint(53.9, 27.5)),
        ContributionMapCanvas { _, _, _ -> }, {}, {}, {}, {}, {})
}
@Preview @Composable private fun ContributionMapLightPreview() = MapPickerPreview(false)
@Preview @Composable private fun ContributionMapDarkPreview() = MapPickerPreview(true)
