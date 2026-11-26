package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.ShelfArchiveViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveState
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfProfileVisibilityState
import kotlinx.coroutines.flow.take
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Archive requests live only while the settings sheet is presented. */
@Composable
internal fun ShelfSettingsSheet(
    archiveModel: () -> ShelfArchiveViewModel, image: ShelfImage, visibilitySettings: @Composable () -> Unit,
    onDismiss: () -> Unit, onOpenBag: (String) -> Unit,
) {
    val owner = remember { ShelfSettingsStoreOwner() }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val model = viewModel(viewModelStoreOwner = owner) { archiveModel() }
    val state by model.state.collectAsStateWithLifecycle()
    ShelfSettingsEvents(model, onDismiss, onOpenBag)
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.shelf_cancel),
        maxSheetHeightFraction = .8f, expandToMaxHeight = true) {
        ShelfSettingsContent(state, model::onAction, image, onDismiss, visibilitySettings)
    }
}

private class ShelfSettingsStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

@Composable
private fun ShelfSettingsEvents(model: ShelfArchiveViewModel, onDismiss: () -> Unit, onOpenBag: (String) -> Unit) {
    val dismiss by rememberUpdatedState(onDismiss)
    val openBag by rememberUpdatedState(onOpenBag)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            model.events.take(1).collect { event ->
                dismiss()
                if (event is ShelfArchiveEvent.OpenBag) openBag(event.id)
            }
        }
    }
}

/** A single scrolling surface keeps settings and search usable in small windows and above the IME. */
@Composable
internal fun ShelfSettingsContent(
    state: ShelfArchiveState, onAction: (ShelfArchiveAction) -> Unit, image: ShelfImage,
    onDismiss: () -> Unit, visibilitySettings: @Composable () -> Unit, modifier: Modifier = Modifier,
) {
    ShelfArchiveList(state, onAction, image, modifier) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ShelfSettingsHeading(onDismiss)
            visibilitySettings()
            HorizontalDivider()
            Text(stringResource(Res.string.shelf_archive), Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            CoffeePeekSearchField(state.query, { onAction(ShelfArchiveAction.QueryChanged(it)) },
                stringResource(Res.string.shelf_archive_search), stringResource(Res.string.shelf_archive_clear),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ShelfSettingsHeading(onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.shelf_settings), Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        CoffeePeekIconButton(onDismiss, Modifier.size(48.dp), contentDescription = stringResource(Res.string.shelf_cancel)) {
            CoffeePeekIcon(CoffeePeekIcons.Close, null, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ShelfSettingsPreview(dark: Boolean, empty: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    Surface(Modifier.fillMaxWidth().height(600.dp)) {
        ShelfSettingsContent(ShelfArchiveState(previewToday, loading = false,
            items = if (empty) emptyList() else previewArchiveItems()), {}, PreviewShelfImage, {}, visibilitySettings = {
            ShelfProfileVisibilityContent(ShelfProfileVisibilityState(signedIn = true, loaded = true, visible = true), {})
        })
    }
}

@Preview @Composable private fun ShelfSettingsLightPreview() = ShelfSettingsPreview(false)
@Preview @Composable private fun ShelfSettingsDarkPreview() = ShelfSettingsPreview(true)
@Preview @Composable private fun ShelfSettingsEmptyLightPreview() = ShelfSettingsPreview(false, empty = true)
@Preview @Composable private fun ShelfSettingsEmptyDarkPreview() = ShelfSettingsPreview(true, empty = true)
