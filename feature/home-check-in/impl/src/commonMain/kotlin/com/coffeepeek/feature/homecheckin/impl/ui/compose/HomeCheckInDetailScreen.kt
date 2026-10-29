@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.homecheckin.impl.ui.compose

import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_close
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_note
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInDetailViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.component.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*

@Composable
internal fun HomeCheckInDetailScreen(
    viewModel: HomeCheckInDetailViewModel,
    accessAllowed: Boolean,
    formatPreparedAt: (String, String) -> String,
    formatBrewMethod: (String) -> String,
    imageContent: @Composable (String, String, Modifier, Boolean) -> Unit,
    modifier: Modifier,
    notice: String?,
    onDismissAllowedChange: (Boolean) -> Unit,
    onEvent: (HomeCheckInDetailEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentAccess by rememberUpdatedState(accessAllowed)
    val currentEvent by rememberUpdatedState(onEvent)
    val currentDismissAllowed by rememberUpdatedState(onDismissAllowedChange)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { envelope ->
                if (currentAccess && viewModel.canDeliver(envelope)) currentEvent(envelope.event)
            }
        }
    }
    LaunchedEffect(viewModel, accessAllowed) {
        if (!accessAllowed) viewModel.onAction(HomeCheckInDetailAction.RevokeAccess)
    }
    // Do not wait for a coroutine/collector before hiding the old account's photos.
    val visibleState = if (accessAllowed) state else HomeCheckInDetailState(isAccessRevoked = true)
    LaunchedEffect(visibleState.isDeleting) { currentDismissAllowed(!visibleState.isDeleting) }
    DisposableEffect(viewModel) { onDispose { currentDismissAllowed(true) } }
    CoffeePeekBackHandler(enabled = !visibleState.isClosed) {
        // Closing an already revoked destination is safe; private effects remain filtered above.
        if (visibleState.isAccessRevoked) currentEvent(HomeCheckInDetailEvent.Close)
        else viewModel.onAction(if (visibleState.showDeleteConfirmation) HomeCheckInDetailAction.CancelDelete else HomeCheckInDetailAction.Close)
    }
    HomeCheckInDetailScreenContent(visibleState, viewModel::onAction, formatPreparedAt, formatBrewMethod,
        imageContent, modifier, notice)
}

/** Body only: the app owns sheet anchors, insets and dismissal. No second nested sheet. */
@Composable
internal fun HomeCheckInDetailScreenContent(
    state: HomeCheckInDetailState,
    onAction: (HomeCheckInDetailAction) -> Unit,
    formatPreparedAt: (String, String) -> String,
    formatBrewMethod: (String) -> String,
    imageContent: @Composable (String, String, Modifier, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    if (state.isClosed || state.isAccessRevoked) return
    val entry = state.entry
    val enabled = !state.isLoading && !state.isDeleting && !state.showDeleteConfirmation
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (state.isLoading || state.isDeleting || state.loadIssue != null || state.deleteIssue != null) item(key = "status") {
            HomeCheckInDetailStatus(state.isLoading, state.isDeleting, state.deleteIssue ?: state.loadIssue,
                state.deleteIssue != null, { onAction(HomeCheckInDetailAction.Reload) }, Modifier.fillMaxWidth())
        }
        if (entry != null) {
            item(key = "heading") { HomeCheckInHeading(entry, formatPreparedAt(entry.preparedOn, entry.preparedAtUtc)) }
            notice?.let { message -> item(key = "notice") {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                    Text(message, Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            } }
            item(key = "recipe") { HomeCheckInRecipeCard(entry, entry.brewMethodCode?.let(formatBrewMethod)) }
            entry.tasteScore?.let { score -> item(key = "rating") { HomeCheckInTasteRating(score) } }
            val urls = entry.photos.map { it.thumbnailUrl ?: it.url }
            if (urls.isNotEmpty()) item(key = "photos") {
                HomeCheckInPhotos(urls, enabled, imageContent, originalUrls = entry.photos.map { it.url }) {
                    onAction(HomeCheckInDetailAction.OpenPhoto(it))
                }
            }
            entry.text?.takeIf(String::isNotBlank)?.let { note -> item(key = "note") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.home_check_in_note), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(note, style = MaterialTheme.typography.bodyLarge)
                }
            } }
            item(key = "actions") {
                HomeCheckInDetailActions(enabled, { onAction(HomeCheckInDetailAction.Edit) }, { onAction(HomeCheckInDetailAction.RequestDelete) })
            }
        } else if (!state.isDeleting) item(key = "close") {
            TextButton(onClick = { onAction(HomeCheckInDetailAction.Close) }) { Text(stringResource(Res.string.home_check_in_close)) }
        }
    }
    if (state.showDeleteConfirmation && entry != null && !state.isDeleting) {
        HomeCheckInDeleteConfirmation({ onAction(HomeCheckInDetailAction.ConfirmDelete) }, { onAction(HomeCheckInDetailAction.CancelDelete) })
    }
}

internal fun previewHomePreparation() = HomePreparation(
    id = "fixture-home", preparedOn = "2026-10-17", preparedAtUtc = "2026-10-17T07:30:00Z", createdAtUtc = "2026-10-17T07:30:00Z",
    text = "Ягодная кислотность и сладкое послевкусие", brewMethodCode = "hario-v60", coffeeProductId = null,
    beanName = "Эфиопия", doseGrams = "18.00", brewDurationSeconds = 180, tasteScore = 4,
    drinkName = "Фильтр", drinkSlug = "filter", customDrinkName = null, photos = emptyList(),
    visibility = HomePreparationVisibility.Private, moderation = HomePreparationModeration.NotSubmitted,
)

@Preview @Composable private fun HomeCheckInDetailPreviewLight() = HomeCheckInDetailPreview(false)
@Preview @Composable private fun HomeCheckInDetailPreviewDark() = HomeCheckInDetailPreview(true)

@Composable private fun HomeCheckInDetailPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInDetailScreenContent(HomeCheckInDetailState(entry = previewHomePreparation()), {},
        { _, _ -> "17.10.2026 · 10:30" }, { "Hario V60" }, { _, _, _, _ -> }) }
}
@Preview @Composable private fun HomeCheckInDetailLightPreview() = HomeCheckInDetailPreview(false)
@Preview @Composable private fun HomeCheckInDetailDarkPreview() = HomeCheckInDetailPreview(true)

@Preview @Composable private fun HomeCheckInDetailLoadingPreviewLight() = HomeCheckInDetailLoadingPreview(false)
@Preview @Composable private fun HomeCheckInDetailLoadingPreviewDark() = HomeCheckInDetailLoadingPreview(true)

@Composable private fun HomeCheckInDetailLoadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInDetailScreenContent(HomeCheckInDetailState(isLoading = true), {}, { date, _ -> date }, { it }, { _, _, _, _ -> }) }
}
@Preview @Composable private fun HomeCheckInDetailLoadingLightPreview() = HomeCheckInDetailLoadingPreview(false)
@Preview @Composable private fun HomeCheckInDetailLoadingDarkPreview() = HomeCheckInDetailLoadingPreview(true)

@Preview @Composable private fun HomeCheckInDetailPhotoVariantsPreviewLight() = HomeCheckInDetailPhotoVariantsPreview(false)
@Preview @Composable private fun HomeCheckInDetailPhotoVariantsPreviewDark() = HomeCheckInDetailPhotoVariantsPreview(true)

@Composable private fun HomeCheckInDetailPhotoVariantsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInDetailScreenContent(HomeCheckInDetailState(entry = previewHomePreparation().copy(
        photos = listOf(HomePreparationPhoto("a", "fixture-original-a", thumbnailUrl = "fixture-feed-a"),
            HomePreparationPhoto("b", "fixture-original-b", thumbnailUrl = "")))), {},
        { _, _ -> "17.10.2026 · 10:30" }, { "Hario V60" }, { _, _, modifier, _ ->
            Box(modifier) // Fake image port: previews never access private images or a network loader.
        }) }
}
@Preview @Composable private fun HomeCheckInDetailPhotoVariantsLightPreview() = HomeCheckInDetailPhotoVariantsPreview(false)
@Preview @Composable private fun HomeCheckInDetailPhotoVariantsDarkPreview() = HomeCheckInDetailPhotoVariantsPreview(true)

@Preview @Composable private fun HomeCheckInDetailErrorPreviewLight() = HomeCheckInDetailErrorPreview(false)
@Preview @Composable private fun HomeCheckInDetailErrorPreviewDark() = HomeCheckInDetailErrorPreview(true)

@Composable private fun HomeCheckInDetailErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInDetailScreenContent(HomeCheckInDetailState(loadIssue = HomePreparationIssue.Offline), {},
        { date, _ -> date }, { it }, { _, _, _, _ -> }) }
}
@Preview @Composable private fun HomeCheckInDetailErrorLightPreview() = HomeCheckInDetailErrorPreview(false)
@Preview @Composable private fun HomeCheckInDetailErrorDarkPreview() = HomeCheckInDetailErrorPreview(true)
