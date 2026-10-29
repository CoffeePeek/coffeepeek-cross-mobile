@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.ImageBitmap
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
    // Screens cross-fade; steps that are one screen (scanning, then the card peeling off) do not. While the scanner is
    // still being checked (null) the screen stays black like a camera that is starting, so nothing else flashes first.
    val shown = state.step.takeUnless { it == ShelfAddStep.Intro && state.scanner == ShelfScannerStatus.Checking }
    AnimatedContent(shown, modifier, transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(320)) },
        contentKey = { it?.screen }, label = "shelf-add") { step ->
        when (step) {
            null -> Box(Modifier.fillMaxSize().background(Color.Black))
            ShelfAddStep.Camera -> ShelfCameraStep(camera, openGallery, viewModel::onAction, Modifier)
            else -> ShelfAddScreenContent(state, viewModel::onAction, openGallery, step = step)
        }
    }
}

private val ShelfAddStep.screen: ShelfAddStep get() = if (this == ShelfAddStep.Revealing) ShelfAddStep.Scanning else this

/** Holds the camera session while the camera is shown; system back returns to where the camera was opened from. */
@Composable
private fun ShelfCameraStep(camera: ShelfCamera, onGallery: () -> Unit, onAction: (ShelfAddAction) -> Unit, modifier: Modifier) {
    val session = camera.rememberSession()
    val scope = rememberCoroutineScope()
    var capturing by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<ImageBitmap?>(null) }
    BackHandler { onAction(ShelfAddAction.CloseCamera) }
    val actions = remember(session, onGallery) {
        ShelfCameraActions(
            onShutter = {
                if (!capturing) {
                    capturing = true
                    frozen = session.snapshot()
                    scope.launch {
                        val photo = session.capture()
                        // On success the frozen frame and the spinner stay while this screen fades out into the scan.
                        if (photo != null) onAction(ShelfAddAction.PhotoPicked(photo))
                        else {
                            capturing = false
                            frozen = null
                            onAction(ShelfAddAction.PhotoFailed)
                        }
                    }
                }
            },
            onGallery = onGallery, onTorch = session::setTorch, onFocus = session::focusAt, onAllow = session::requestAccess,
            onClose = { onAction(ShelfAddAction.CloseCamera) },
        )
    }
    ShelfCameraContent(session.status, session.torchAvailable, session.torch, session.focus, capturing, frozen, { session.Viewfinder(it) },
        actions, modifier)
}

@Composable
internal fun ShelfAddScreenContent(
    state: ShelfAddState, onAction: (ShelfAddAction) -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier,
    /** The step this content shows; while a screen fades out it keeps its own step. */
    step: ShelfAddStep = state.step,
) {
    val snackbar = remember { SnackbarHostState() }
    ShelfAddMessageEffect(state.message, snackbar, onAction)
    var origin by remember { mutableStateOf(Offset.Zero) }
    var photoBounds by remember { mutableStateOf<Rect?>(null) }
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        CoffeePeekScaffold(Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
            CoffeePeekTopBar(stringResource(Res.string.shelf_add_title), stringResource(Res.string.shelf_add_close),
                onBack = { onAction(ShelfAddAction.Close) })
        }) { padding ->
            ShelfAddStepBody(state, step, onAction, onGallery, Modifier.padding(padding), { photoBounds = it }, { cardBounds = it })
        }
        // Above the top bar too: the card flies off the screen and drops back in from beyond its edge.
        ShelfFlyingCard(state, step, photoBounds?.translate(-origin), cardBounds?.translate(-origin), onAction)
    }
    if (state.confirmDuplicate) CoffeePeekActionDialog(stringResource(Res.string.shelf_duplicate_title),
        stringResource(Res.string.shelf_duplicate_message),
        listOf(CoffeePeekDialogAction(stringResource(Res.string.shelf_duplicate_confirm), { onAction(ShelfAddAction.ConfirmDuplicate) })),
        stringResource(Res.string.shelf_cancel)) { onAction(ShelfAddAction.CancelDuplicate) }
}

@Composable
private fun ShelfAddStepBody(
    state: ShelfAddState, step: ShelfAddStep, onAction: (ShelfAddAction) -> Unit, onGallery: () -> Unit, modifier: Modifier,
    onPhotoBounds: (Rect) -> Unit, onCardBounds: (Rect) -> Unit,
) {
    val openCamera = { onAction(ShelfAddAction.OpenCamera) }
    val photo = state.photo
    when {
        step == ShelfAddStep.Intro || photo == null ->
            ShelfScanIntro(state.scanner, openCamera, onGallery, { onAction(ShelfAddAction.DownloadScanner) }, modifier)
        step == ShelfAddStep.Scanning || step == ShelfAddStep.Revealing -> ShelfScanProgress(photo.bytes, state.stagesDone,
            state.liveFrame ?: state.result?.frame, revealing = step == ShelfAddStep.Revealing, onPhotoBounds, modifier)
        step == ShelfAddStep.Retake -> ShelfScanRetake(photo.bytes, state.result?.frame, state.stageOutcomes(), state.failure,
            openCamera, onGallery, modifier)
        else -> ShelfBagFormContent(state, onAction, openCamera, modifier, onCardBounds)
    }
}

/** The accepted card on its way from the photo, off the top of the screen and down into its place in the form. */
@Composable
private fun ShelfFlyingCard(state: ShelfAddState, step: ShelfAddStep, photo: Rect?, slot: Rect?, onAction: (ShelfAddAction) -> Unit) {
    val result = state.result ?: return
    val card = result.card ?: return
    val aspect = result.cardWidth.toFloat() / result.cardHeight.coerceAtLeast(1)
    when {
        step == ShelfAddStep.Revealing && photo != null ->
            ShelfCardPeel(card.bytes, aspect, state.liveFrame ?: result.frame, photo, { onAction(ShelfAddAction.RevealFinished) })
        step == ShelfAddStep.Form && state.cardLanding && slot != null ->
            ShelfCardLanding(card.bytes, slot, { onAction(ShelfAddAction.CardLanded) })
    }
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
