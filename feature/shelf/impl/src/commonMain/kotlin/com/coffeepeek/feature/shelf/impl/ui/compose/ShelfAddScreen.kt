@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.backhandler.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekActionDialog
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.api.ShelfCamera
import com.coffeepeek.feature.shelf.api.ShelfGalleryPicker
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.ShelfAddViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.component.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Lifecycle adapter: the application's camera and gallery feed photos into the model; the camera runs only on its step. */
@Composable
internal fun ShelfAddScreen(
    viewModel: ShelfAddViewModel, gallery: ShelfGalleryPicker, camera: ShelfCamera, modifier: Modifier, onEvent: (ShelfAddEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    val openGallery = gallery({ viewModel.onAction(ShelfAddAction.PhotoPicked(it)) }, { viewModel.onAction(ShelfAddAction.PhotoFailed) })
    if (state.step == ShelfAddStep.Camera) ShelfCameraStep(camera, openGallery, viewModel::onAction, modifier)
    else ShelfAddScreenContent(state, viewModel::onAction, openGallery, modifier)
}

/** Holds the camera session while the camera is shown; system back returns to where the camera was opened from. */
@Composable
private fun ShelfCameraStep(camera: ShelfCamera, onGallery: () -> Unit, onAction: (ShelfAddAction) -> Unit, modifier: Modifier) {
    val session = camera.rememberSession()
    val scope = rememberCoroutineScope()
    var capturing by remember { mutableStateOf(false) }
    BackHandler { onAction(ShelfAddAction.CloseCamera) }
    val actions = remember(session, onGallery) {
        ShelfCameraActions(
            onShutter = {
                if (!capturing) {
                    capturing = true
                    scope.launch {
                        val photo = session.capture()
                        capturing = false
                        onAction(photo?.let { ShelfAddAction.PhotoPicked(it) } ?: ShelfAddAction.PhotoFailed)
                    }
                }
            },
            onGallery = onGallery, onTorch = session::setTorch, onFocus = session::focusAt, onAllow = session::requestAccess,
            onClose = { onAction(ShelfAddAction.CloseCamera) },
        )
    }
    ShelfCameraContent(session.status, session.torchAvailable, session.torch, session.focus, capturing, { session.Viewfinder(it) }, actions, modifier)
}

@Composable
internal fun ShelfAddScreenContent(state: ShelfAddState, onAction: (ShelfAddAction) -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier) {
    val openCamera = { onAction(ShelfAddAction.OpenCamera) }
    val snackbar = remember { SnackbarHostState() }
    ShelfAddMessageEffect(state.message, snackbar, onAction)
    CoffeePeekScaffold(modifier, snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        CoffeePeekTopBar(stringResource(Res.string.shelf_add_title), stringResource(Res.string.shelf_add_close),
            onBack = { onAction(ShelfAddAction.Close) })
    }) { padding ->
        val content = Modifier.padding(padding)
        val photo = state.photo
        when {
            state.step == ShelfAddStep.Intro || photo == null ->
                ShelfScanIntro(state.scanner, openCamera, onGallery, { onAction(ShelfAddAction.DownloadScanner) }, content)
            state.step == ShelfAddStep.Scanning -> ShelfScanProgress(photo.bytes, state.stagesDone, content)
            state.step == ShelfAddStep.Retake -> ShelfScanRetake(photo.bytes, state.result?.frame, state.stageOutcomes(), state.failure,
                openCamera, onGallery, content)
            else -> ShelfBagFormContent(state, onAction, openCamera, content)
        }
    }
    if (state.confirmDuplicate) CoffeePeekActionDialog(stringResource(Res.string.shelf_duplicate_title),
        stringResource(Res.string.shelf_duplicate_message),
        listOf(CoffeePeekDialogAction(stringResource(Res.string.shelf_duplicate_confirm), { onAction(ShelfAddAction.ConfirmDuplicate) })),
        stringResource(Res.string.shelf_cancel)) { onAction(ShelfAddAction.CancelDuplicate) }
}

@Composable
private fun ShelfAddMessageEffect(message: ShelfAddMessage?, snackbar: SnackbarHostState, onAction: (ShelfAddAction) -> Unit) {
    val text = when (message) {
        null -> null
        ShelfAddMessage.PhotoUnreadable -> stringResource(Res.string.shelf_scan_failed_photo)
        is ShelfAddMessage.Failed -> message.issue.text()
    }
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(message) {
        if (text == null) return@LaunchedEffect
        snackbar.showSnackbar(text)
        currentAction(ShelfAddAction.DismissMessage)
    }
}

private val previewPhoto = ShelfPhoto(byteArrayOf(), fromCamera = true)

private fun previewResult(status: PackModerationStatus, reasons: List<PackModerationReason>) = BagScanResult(
    card = ShelfBagCard(byteArrayOf(), "image/jpeg", "bag.jpg"), cardWidth = 720, cardHeight = 960, frame = null, quality = null,
    moderation = PackModeration(status, reasons), timings = BagScanTimings(0, 0, 0, 0, 0),
)

@Composable
private fun ShelfAddPreview(dark: Boolean, state: ShelfAddState) = CoffeePeekTheme(darkTheme = dark) {
    ShelfAddScreenContent(state, {}, {})
}

@Preview @Composable private fun ShelfAddIntroLightPreview() = ShelfAddPreview(false, ShelfAddState(previewToday, scanner = ShelfScannerStatus.Ready))
@Preview @Composable private fun ShelfAddIntroDarkPreview() = ShelfAddPreview(true, ShelfAddState(previewToday, scanner = ShelfScannerStatus.Ready))
@Preview @Composable private fun ShelfAddRetakeLightPreview() = ShelfAddPreview(false, ShelfAddState(previewToday, ShelfAddStep.Retake,
    photo = previewPhoto, stagesDone = BagScanStage.entries.toSet(),
    result = previewResult(PackModerationStatus.Retake, listOf(PackModerationReason.ObjectsOnBag))))
@Preview @Composable private fun ShelfAddRetakeDarkPreview() = ShelfAddPreview(true, ShelfAddState(previewToday, ShelfAddStep.Retake,
    photo = previewPhoto, stagesDone = BagScanStage.entries.toSet(),
    result = previewResult(PackModerationStatus.Retake, listOf(PackModerationReason.ObjectsOnBag))))
@Preview @Composable private fun ShelfAddFormLightPreview() = ShelfAddPreview(false, ShelfAddState(previewToday, ShelfAddStep.Form,
    photo = previewPhoto, result = previewResult(PackModerationStatus.AcceptedWithHint, listOf(PackModerationReason.Glare))))
@Preview @Composable private fun ShelfAddFormDarkPreview() = ShelfAddPreview(true, ShelfAddState(previewToday, ShelfAddStep.Form,
    photo = previewPhoto, result = previewResult(PackModerationStatus.AcceptedWithHint, listOf(PackModerationReason.Glare))))
