package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.domain.model.ShelfBag
import com.coffeepeek.feature.shelf.domain.model.ShelfBagPhoto
import com.coffeepeek.feature.shelf.domain.model.ShelfBagStatus
import com.coffeepeek.feature.shelf.domain.model.ShelfGrind
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.ShelfViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.component.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfMessage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfState
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Lifecycle adapter: the shelf loads whenever a signed-in user opens it. */
@Composable
internal fun ShelfScreen(
    viewModel: ShelfViewModel, signedIn: Boolean, image: ShelfImage, bottomClearance: Dp, modifier: Modifier,
    header: @Composable ColumnScope.() -> Unit, onEvent: (ShelfEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    LaunchedEffect(viewModel, signedIn) { if (signedIn) viewModel.onAction(ShelfAction.Refresh) }
    ShelfScreenContent(state, signedIn, viewModel::onAction, image, modifier, bottomClearance, header)
}

@Composable
internal fun ShelfScreenContent(
    state: ShelfState, signedIn: Boolean, onAction: (ShelfAction) -> Unit, image: ShelfImage,
    modifier: Modifier = Modifier, bottomClearance: Dp = 0.dp, header: @Composable ColumnScope.() -> Unit = {},
) {
    val drag = remember { ShelfDragState() }
    val snackbar = remember { SnackbarHostState() }
    ShelfMessageEffect(state.message, snackbar, onAction)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = CoffeePeekDimens.spacing4).padding(top = CoffeePeekDimens.spacing3)) {
                header()
                ShelfSummaryRow(state.summary.takeIf { signedIn && state.loaded }, drag.active,
                    { onAction(ShelfAction.OpenArchive) }.takeIf { signedIn })
            }
            ShelfBody(state, signedIn, drag, image, onAction, bottomClearance, Modifier.weight(1f))
        }
        if (drag.active) ShelfFinishZone(drag.overZone, Modifier.align(Alignment.BottomCenter)
            .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = bottomClearance + CoffeePeekDimens.spacing3)
            .onGloballyPositioned { drag.zone = it.boundsInRoot() })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = bottomClearance))
    }
    ShelfSheets(state, image, onAction)
}

@Composable
private fun ShelfBody(
    state: ShelfState, signedIn: Boolean, drag: ShelfDragState, image: ShelfImage,
    onAction: (ShelfAction) -> Unit, bottomClearance: Dp, modifier: Modifier,
) {
    val scrolling = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = bottomClearance + 96.dp)
    when {
        !signedIn -> ShelfInvitation(stringResource(Res.string.shelf_guest_title), stringResource(Res.string.shelf_guest_body),
            stringResource(Res.string.shelf_sign_in), { onAction(ShelfAction.SignIn) }, modifier.then(scrolling))
        !state.loaded && state.loadFailed -> ShelfLoadError(stringResource(Res.string.shelf_load_failed),
            { onAction(ShelfAction.Refresh) }, modifier.then(scrolling))
        !state.loaded -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CoffeePeekLoader(stringResource(Res.string.shelf_loading))
        }
        state.bags.isEmpty() -> ShelfInvitation(stringResource(Res.string.shelf_empty_title), stringResource(Res.string.shelf_empty_body),
            stringResource(Res.string.shelf_browse_catalog), { onAction(ShelfAction.BrowseCatalog) }, modifier.then(scrolling))
        else -> ShelfShelves(state.bags, drag, image, onAction, bottomClearance, modifier)
    }
}

@Composable
private fun ShelfShelves(
    bags: List<ShelfBagItem>, drag: ShelfDragState, image: ShelfImage,
    onAction: (ShelfAction) -> Unit, bottomClearance: Dp, modifier: Modifier,
) {
    val scroll = rememberScrollState()
    val viewport = remember { arrayOfNulls<Rect>(1) }
    ShelfAutoScroll(drag, scroll) { viewport[0] }
    LaunchedEffect(bags) { drag.settled() }
    LaunchedEffect(drag.settling) { if (drag.settling != null) { delay(1_000); drag.settled() } }
    Box(modifier.fillMaxSize().onGloballyPositioned { viewport[0] = it.boundsInRoot() }.verticalScroll(scroll)
        .padding(horizontal = CoffeePeekDimens.spacing4).padding(top = CoffeePeekDimens.spacing4, bottom = bottomClearance + 96.dp)) {
        ShelfGrid(bags, drag, image, onAction)
    }
}

/** Scrolls the shelf while a lifted bag is held near the top or bottom edge. */
@Composable
private fun ShelfAutoScroll(drag: ShelfDragState, scroll: ScrollState, viewport: () -> Rect?) {
    val edge = with(LocalDensity.current) { 64.dp.toPx() }
    val step = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(drag.active) {
        while (drag.active) {
            withFrameNanos { }
            val bounds = viewport() ?: continue
            val bottom = minOf(bounds.bottom, drag.zone?.top ?: bounds.bottom)
            val y = drag.rootPointer.y
            val delta = when {
                y < bounds.top + edge -> -step
                y > bottom - edge && y < bottom -> step
                else -> 0f
            }
            if (delta != 0f) drag.scrolled(scroll.scrollBy(delta))
        }
    }
}

/** Shows the current message; "Убрали в архив" offers undo for five seconds. */
@Composable
private fun ShelfMessageEffect(message: ShelfMessage?, snackbar: SnackbarHostState, onAction: (ShelfAction) -> Unit) {
    val text = when (message) {
        null -> null
        is ShelfMessage.Finished -> stringResource(Res.string.shelf_finished_message)
        ShelfMessage.Restored -> stringResource(Res.string.shelf_restored)
        ShelfMessage.Deleted -> stringResource(Res.string.shelf_deleted)
        is ShelfMessage.Failed -> message.issue.text()
    }
    val undo = stringResource(Res.string.shelf_undo)
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(message) {
        if (message == null || text == null) return@LaunchedEffect
        val finished = message as? ShelfMessage.Finished
        val result = if (finished == null) snackbar.showSnackbar(text)
            else withTimeoutOrNull(5_000) { snackbar.showSnackbar(text, undo, duration = SnackbarDuration.Indefinite) }
        if (finished != null && result == SnackbarResult.ActionPerformed) currentAction(ShelfAction.UndoFinish(finished.bagId, finished.position))
        else currentAction(ShelfAction.DismissMessage)
    }
}

@Composable
private fun ShelfSheets(state: ShelfState, image: ShelfImage, onAction: (ShelfAction) -> Unit) {
    state.openBag?.let { item ->
        ShelfBagSheet(item, state.today, image, ShelfBagSheetMode.Shelf, onPrimary = { onAction(ShelfAction.StartFinish(item.id)) },
            onDelete = { onAction(ShelfAction.RequestDelete(item.id)) }, onDismiss = { onAction(ShelfAction.CloseBag) })
    }
    state.finishing?.let { item ->
        ShelfFinishSheet(item, state.today, { rating, buyAgain -> onAction(ShelfAction.Finish(item.id, rating, buyAgain)) },
            onDismiss = { onAction(ShelfAction.CancelFinish) })
    }
    state.deletingId?.let { id -> ShelfDeleteConfirmation({ onAction(ShelfAction.Delete(id)) }, { onAction(ShelfAction.CancelDelete) }) }
}

internal val previewToday = ShelfDay(2026, 10, 27)

/** Previews draw cards as the brand placeholder; the app supplies the real loader. */
internal val PreviewShelfImage: ShelfImage = { _, _, _, modifier -> CoffeePeekImagePlaceholder(modifier, labelSize = 12.sp) }

internal fun previewBag(id: String, name: String?, roaster: String?, grams: Int?, photo: Boolean = true, finished: Boolean = false) = ShelfBag(
    id = id, status = if (finished) ShelfBagStatus.Finished else ShelfBagStatus.OnShelf, position = null,
    photo = ShelfBagPhoto(id, "https://preview.invalid/$id").takeIf { photo }, coffeeProductId = null, coffeeProductSlug = null,
    name = name, roasterName = roaster, weightGrams = grams, grind = ShelfGrind.Beans, roastedOn = "2026-10-06",
    addedAtUtc = "2026-10-18T09:00:00Z", finishedAtUtc = "2026-10-25T09:00:00Z".takeIf { finished },
    rating = 4.takeIf { finished }, buyAgain = true.takeIf { finished },
)

internal fun previewShelfItems(): List<ShelfBagItem> = listOf(
    previewBag("a", "Эфиопия Гуджи", "River Roasters", 250),
    previewBag("b", "Колумбия P!X", "P!X", 250),
    previewBag("c", "Бразилия", "Sibaristica", 1000),
    previewBag("d", "Кения Ньери", null, null, photo = false),
).map { ShelfBagItem(it, ShelfDay(2026, 10, 18), null, ShelfDay(2026, 10, 6)) }

@Composable
private fun ShelfScreenPreview(dark: Boolean, state: ShelfState, signedIn: Boolean = true) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScreenContent(state, signedIn, {}, PreviewShelfImage)
}

@Preview @Composable private fun ShelfLightPreview() = ShelfScreenPreview(false, ShelfState(previewToday, previewShelfItems(), loaded = true))
@Preview @Composable private fun ShelfDarkPreview() = ShelfScreenPreview(true, ShelfState(previewToday, previewShelfItems(), loaded = true))
@Preview @Composable private fun ShelfEmptyLightPreview() = ShelfScreenPreview(false, ShelfState(previewToday, loaded = true))
@Preview @Composable private fun ShelfEmptyDarkPreview() = ShelfScreenPreview(true, ShelfState(previewToday, loaded = true))
@Preview @Composable private fun ShelfGuestLightPreview() = ShelfScreenPreview(false, ShelfState(previewToday), signedIn = false)
@Preview @Composable private fun ShelfGuestDarkPreview() = ShelfScreenPreview(true, ShelfState(previewToday), signedIn = false)
