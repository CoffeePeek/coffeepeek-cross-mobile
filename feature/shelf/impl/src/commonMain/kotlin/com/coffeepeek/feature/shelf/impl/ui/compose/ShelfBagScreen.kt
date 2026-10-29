package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekActionDialog
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfBrewCoffee
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.ShelfBagViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.component.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagState
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The application's home check-in form for [ShelfBrewCoffee], over the bag's screen until it closes. */
internal typealias ShelfBrewForm = @Composable (coffee: ShelfBrewCoffee, onClose: (saved: Boolean) -> Unit) -> Unit

/** Lifecycle adapter for a bag's screen; without [brew] the screen offers no "Заварить чашку". */
@Composable
internal fun ShelfBagScreen(
    viewModel: ShelfBagViewModel, image: ShelfImage, brew: ShelfBrewForm?, modifier: Modifier, onEvent: (ShelfBagEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    ShelfBagScreenContent(state, viewModel::onAction, image, canBrew = brew != null, modifier)
    val coffee = state.brewing
    if (brew != null && coffee != null) brew(coffee) { saved -> viewModel.onAction(ShelfBagAction.BrewClosed(saved)) }
}

/**
 * Mockup K: back and "⋯" above, the bag below and its main action pinned at the bottom: "Заварить чашку" on the shelf
 * ("Закончилась" when brewing is not offered), "Вернуть на полку" in the archive. "⋯" holds finishing or restoring and deletion.
 */
@Composable
internal fun ShelfBagScreenContent(
    state: ShelfBagState, onAction: (ShelfBagAction) -> Unit, image: ShelfImage, canBrew: Boolean, modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    ShelfBagFailureEffect(state.failure, snackbar, onAction)
    CoffeePeekScaffold(modifier, snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        CoffeePeekTopBar("", stringResource(Res.string.shelf_bag_back), onBack = { onAction(ShelfBagAction.Back) }) {
            if (state.bag != null) IconButton({ menu = true }) {
                Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.shelf_bag_more), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }, bottomBar = { if (state.bag != null) ShelfBagMainButton(state, canBrew, onAction) }) { padding ->
        ShelfBagBody(state, image, onAction, Modifier.padding(padding))
    }
    if (menu) ShelfBagMenu(state, onAction) { menu = false }
    ShelfBagDialogs(state, image, onAction)
}

@Composable
private fun ShelfBagBody(state: ShelfBagState, image: ShelfImage, onAction: (ShelfBagAction) -> Unit, modifier: Modifier) {
    val item = state.bag
    when {
        item != null -> ShelfBagDetails(item, state.brews, state.today, image, state.archived,
            onOpenCoffee = { onAction(ShelfBagAction.OpenCoffee) }.takeIf { item.bag.coffeeProductSlug != null },
            onRetryBrews = { onAction(ShelfBagAction.RetryBrews) }, modifier = modifier)
        state.missing -> ShelfInvitation(stringResource(Res.string.shelf_bag_missing_title), stringResource(Res.string.shelf_bag_missing_body),
            stringResource(Res.string.shelf_bag_back), { onAction(ShelfBagAction.Back) }, modifier.padding(CoffeePeekDimens.spacing4))
        else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CoffeePeekLoader(stringResource(Res.string.shelf_loading)) }
    }
}

@Composable
private fun ShelfBagMainButton(state: ShelfBagState, canBrew: Boolean, onAction: (ShelfBagAction) -> Unit) {
    val (label, action) = when {
        state.archived -> Res.string.shelf_bag_restore to ShelfBagAction.Restore
        canBrew -> Res.string.shelf_brew_action to ShelfBagAction.Brew
        else -> Res.string.shelf_finish_action to ShelfBagAction.StartFinish
    }
    CoffeePeekButton(stringResource(label), { onAction(action) }, Modifier.navigationBarsPadding()
        .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3), enabled = !state.busy)
}

/** "Закончилась" or "Вернуть на полку", then "Удалить". */
@Composable
private fun ShelfBagMenu(state: ShelfBagState, onAction: (ShelfBagAction) -> Unit, onDismiss: () -> Unit) {
    val item = state.bag ?: return
    val main = if (state.archived) CoffeePeekDialogAction(stringResource(Res.string.shelf_bag_restore), { onDismiss(); onAction(ShelfBagAction.Restore) })
        else CoffeePeekDialogAction(stringResource(Res.string.shelf_finish_action), { onDismiss(); onAction(ShelfBagAction.StartFinish) })
    CoffeePeekActionDialog(item.title(), null, listOf(main,
        CoffeePeekDialogAction(stringResource(Res.string.shelf_bag_delete), { onDismiss(); onAction(ShelfBagAction.RequestDelete) }, destructive = true),
    ), stringResource(Res.string.shelf_cancel), onDismiss)
}

@Composable
private fun ShelfBagDialogs(state: ShelfBagState, image: ShelfImage, onAction: (ShelfBagAction) -> Unit) {
    val item = state.bag ?: return
    if (state.finishing) ShelfFinishSheet(item, state.today, image, { rating, buyAgain -> onAction(ShelfBagAction.Finish(rating, buyAgain)) },
        onDismiss = { onAction(ShelfBagAction.CancelFinish) })
    if (state.deleting) ShelfDeleteConfirmation({ onAction(ShelfBagAction.Delete) }, { onAction(ShelfBagAction.CancelDelete) })
}

@Composable
private fun ShelfBagFailureEffect(failure: ShelfIssue?, snackbar: SnackbarHostState, onAction: (ShelfBagAction) -> Unit) {
    val text = failure?.text()
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(failure) {
        if (text == null) return@LaunchedEffect
        snackbar.showSnackbar(text)
        currentAction(ShelfBagAction.DismissFailure)
    }
}

@Composable
private fun ShelfBagScreenPreview(dark: Boolean, state: ShelfBagState) = CoffeePeekTheme(darkTheme = dark) {
    ShelfBagScreenContent(state, {}, PreviewShelfImage, canBrew = true)
}

@Preview @Composable private fun ShelfBagScreenLightPreview() =
    ShelfBagScreenPreview(false, ShelfBagState(previewToday, previewShelfItems()[1], brews = previewBrews()))
@Preview @Composable private fun ShelfBagScreenDarkPreview() =
    ShelfBagScreenPreview(true, ShelfBagState(previewToday, previewShelfItems()[1], brews = previewBrews()))
@Preview @Composable private fun ShelfBagScreenArchivedLightPreview() =
    ShelfBagScreenPreview(false, ShelfBagState(previewToday, previewArchiveItems()[0], brews = ShelfBagBrews.Loaded(emptyList(), null)))
@Preview @Composable private fun ShelfBagScreenArchivedDarkPreview() =
    ShelfBagScreenPreview(true, ShelfBagState(previewToday, previewArchiveItems()[0], brews = ShelfBagBrews.Loaded(emptyList(), null)))
@Preview @Composable private fun ShelfBagScreenMissingLightPreview() = ShelfBagScreenPreview(false, ShelfBagState(previewToday, missing = true))
@Preview @Composable private fun ShelfBagScreenMissingDarkPreview() = ShelfBagScreenPreview(true, ShelfBagState(previewToday, missing = true))
