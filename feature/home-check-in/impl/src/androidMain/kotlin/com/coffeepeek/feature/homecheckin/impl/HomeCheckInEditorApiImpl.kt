package com.coffeepeek.feature.homecheckin.impl

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInEditorViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.HomeCheckInEditorScreen
import com.coffeepeek.feature.homecheckin.impl.ui.compose.component.HomeCheckInPhotoSource
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*

/**
 * Construct per account lease. All ports/repository must guard that same lease independently
 * of Compose updates. Reuses configured picker/media; creates no client or root DI/navigation.
 * Analytics factory adapts one existing tracker (including a global-creation handoff).
 */
fun createHomeCheckInEditorEntry(
    repository: HomePreparationRepository,
    catalog: HomeCheckInEditorCatalog,
    canAccess: () -> Boolean,
    dates: HomeCheckInEditorDates,
    media: HomeCheckInEditorMedia,
    photoPicker: HomeCheckInPhotoPicker,
    analytics: (editing: Boolean) -> HomeCheckInEditorAnalytics = { HomeCheckInEditorAnalytics.None },
): HomeCheckInEditorEntry = HomeCheckInEditorApiImpl(repository, catalog, canAccess, dates, media, photoPicker, analytics)

private class HomeCheckInEditorApiImpl(
    val repository: HomePreparationRepository,
    val catalog: HomeCheckInEditorCatalog,
    val canAccess: () -> Boolean,
    val dates: HomeCheckInEditorDates,
    val media: HomeCheckInEditorMedia,
    val picker: HomeCheckInPhotoPicker,
    val analytics: (Boolean) -> HomeCheckInEditorAnalytics,
) : HomeCheckInEditorEntry {
    @Composable
    override fun Content(preparationId: String?, sessionGeneration: Int, accessAllowed: Boolean,
        onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowedChange: (Boolean) -> Unit, modifier: Modifier) {
        key(this, sessionGeneration, preparationId) {
            val model = viewModel(key = "home-editor:$sessionGeneration:$preparationId") {
                val instant = dates.currentInstant()
                HomeCheckInEditorViewModel(repository, catalog, instant, dates.localDate(instant),
                    preparationId, sessionGeneration, canAccess, analytics = analytics(preparationId != null))
            }
            Editor(model, preparationId, accessAllowed, onClose, onSaved, onDismissAllowedChange, modifier)
        }
    }

    @Composable
    private fun Editor(model: HomeCheckInEditorViewModel, preparationId: String?, accessAllowed: Boolean,
        onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowedChange: (Boolean) -> Unit, modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val allowed = accessAllowed && canAccess()
        val canPick = { allowed && canAccess() && model.state.value.let {
            preparationId == null && it.isReady && !it.isSaving && !it.isClosed && !it.isAccessRevoked
        } }
        val currentCanPick by rememberUpdatedState(canPick)
        val delivery = remember(model) { HomeCheckInPhotoDelivery({ currentCanPick() }) {
            model.onAction(HomeCheckInEditorAction.PhotosAdded(it))
        } }
        DisposableEffect(delivery) { onDispose { delivery.dispose() } }
        var chooseSource by remember { mutableStateOf(false) }
        val controller = if (allowed && !state.isClosed && !state.isAccessRevoked) picker.rememberController() else null
        HomeCheckInEditorScreen(model, allowed, dates.localDate(dates.currentInstant()), dates.formatDate,
            dates.civilDateToInstant, dates.formatMethod, media, controller?.isPreparing == true,
            { if (canPick() && controller?.isPreparing == false) chooseSource = true }, onDismissAllowedChange,
            { event -> when (event) {
                HomeCheckInEditorEvent.Close -> onClose()
                is HomeCheckInEditorEvent.Saved -> onSaved(event.preparationId, event.visibilityUnconfirmed)
            } }, modifier)
        if (chooseSource && controller != null && canPick() && !controller.isPreparing)
            PhotoSource(model, controller, delivery, canPick) { chooseSource = false }
    }

    @Composable
    private fun PhotoSource(model: HomeCheckInEditorViewModel, controller: HomeCheckInPhotoPickerController,
        delivery: HomeCheckInPhotoDelivery, canPick: () -> Boolean, dismiss: () -> Unit) {
        HomeCheckInPhotoSource(onDismiss = dismiss, onGallery = {
            dismiss()
            val remaining = 5 - model.state.value.draft.photos.size
            if (canPick()) delivery.request(remaining)?.let { controller.pickFromGallery(remaining, it) }
        }, onCamera = {
            dismiss()
            if (canPick()) delivery.request(1)?.let(controller.takePhoto)
        })
    }
}
