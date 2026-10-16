package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInUiState
import com.coffeepeek.admin.feature.homecheckin.ui.HomeDetailContent
import com.coffeepeek.admin.feature.homecheckin.ui.HomeEditorContent
import com.coffeepeek.admin.feature.homecheckin.ui.HomeMonth
import com.coffeepeek.admin.ui.component.PlatformAlert
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import org.jetbrains.compose.resources.stringResource

/** Existing Home UI with an Android account-owned lifetime; no Home implementation enters history impl. */
@Composable
internal fun HistoryHomeOverlay(bridge: AndroidHistoryHomeBridge, onPhoto: (List<String>, Int) -> Unit) {
    val editor by bridge.editor.collectAsStateWithLifecycle()
    editor?.let { current ->
        key(current) {
            val state by current.state.collectAsStateWithLifecycle()
            AnalyticsOverlay {
                HistoryHomeOverlayContent(state, bridge::dismiss, bridge::retry,
                    onEdit = { bridge.withEditor(current) { it.edit() } },
                    onRequestDelete = { bridge.withEditor(current) { it.confirmDelete() } },
                    onCancelDelete = { bridge.withEditor(current) { it.cancelDelete() } },
                    onConfirmDelete = { bridge.withEditor(current) { it.delete() } },
                    onPhoto = { urls, index -> bridge.withEditor(current) { onPhoto(urls, index) } }) { modifier ->
                    HomeEditorContent(state,
                        { draft -> bridge.withEditor(current) { it.updateDraft { draft } } },
                        { query -> bridge.withEditor(current) { it.setCatalogQuery(query) } },
                        { bridge.withEditor(current) { it.searchCatalog() } },
                        { product -> bridge.withEditor(current) { it.selectCatalogProduct(product) } },
                        { bridge.withEditor(current) { it.save() } }, bridge::dismiss, modifier,
                        onRetryMethods = { bridge.withEditor(current) { it.loadBrewMethods() } },
                        onSelectDrinkPhoto = { photo -> bridge.withEditor(current) { it.selectDrinkPhoto(photo) } },
                        onRetryDrinkPhoto = { bridge.withEditor(current) { it.retryDrinkPhoto() } },
                        onSkipDrinkPhoto = { bridge.withEditor(current) { it.skipDrinkPhoto() } })
                }
            }
        }
    }
}

@Composable
internal fun HistoryHomeOverlayContent(state: HomeCheckInUiState, onDismiss: () -> Unit, onRetry: () -> Unit,
    onEdit: () -> Unit, onRequestDelete: () -> Unit, onCancelDelete: () -> Unit, onConfirmDelete: () -> Unit,
    onPhoto: (List<String>, Int) -> Unit, editorContent: @Composable (Modifier) -> Unit) {
    val fullHeight = state.editing || state.selected != null
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.history_home_close),
        dismissEnabled = !state.saving, maxSheetHeightFraction = if (fullHeight) 0.9f else null) {
        HistoryHomeBody(state, onRetry, onEdit, onRequestDelete, onPhoto,
            if (fullHeight) Modifier.weight(1f) else Modifier, editorContent)
    }
    if (state.deleteConfirmation) PlatformAlert(
        title = stringResource(Res.string.history_home_delete_title),
        message = stringResource(Res.string.history_home_delete_message),
        confirmLabel = stringResource(Res.string.history_home_delete_confirm),
        cancelLabel = stringResource(Res.string.history_home_delete_cancel),
        onConfirm = onConfirmDelete, onDismiss = onCancelDelete, destructive = true, enabled = !state.saving)
}

@Composable private fun HistoryHomeBody(state: HomeCheckInUiState, onRetry: () -> Unit, onEdit: () -> Unit,
    onRequestDelete: () -> Unit, onPhoto: (List<String>, Int) -> Unit, modifier: Modifier,
    editorContent: @Composable (Modifier) -> Unit) {
    when {
        state.editing -> editorContent(modifier.fillMaxWidth())
        state.selected != null -> Column(modifier.fillMaxWidth()) {
            if (state.error != null) Text(stringResource(Res.string.history_home_action_error),
                Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error)
            if (state.saving) Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.history_home_saving))
            }
            HomeDetailContent(state.selected, Modifier.weight(1f), state.saveNotice, onEdit, onRequestDelete, onPhoto)
        }
        else -> Column(modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.detailLoading) CoffeePeekLoader(stringResource(Res.string.history_home_loading))
            else if (state.error != null) {
                Text(stringResource(Res.string.history_home_load_error), color = MaterialTheme.colorScheme.error)
                CoffeePeekButton(stringResource(Res.string.history_home_retry), onRetry)
            }
        }
    }
}

// Modal placement is tested on-device; IDE previews render its actual stateless body.
@PreviewLightDark @Composable private fun HistoryHomeEditorPreview() = CoffeePeekTheme {
    val state = HomeCheckInUiState(month = HomeMonth(2026, 10), editing = true,
        draft = HomeCheckInInput("2026-10-16T08:00:00Z", "2026-10-16", tasteScore = 5))
    HistoryHomeBody(state, {}, {}, {}, { _, _ -> }, Modifier.height(640.dp)) { modifier ->
        HomeEditorContent(state, {}, {}, {}, {}, {}, {}, modifier)
    }
}
@PreviewLightDark @Composable private fun HistoryHomeFailurePreview() = CoffeePeekTheme {
    HistoryHomeBody(HomeCheckInUiState(month = HomeMonth(2026, 10), error = "Fake failure",
        draft = HomeCheckInInput("2026-10-16T08:00:00Z")), {}, {}, {}, { _, _ -> }, Modifier, {})
}
