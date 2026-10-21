package com.coffeepeek.feature.coffee.impl.ui.compose

import com.coffeepeek.feature.coffee.api.CoffeeExposure
import com.coffeepeek.feature.coffee.api.CoffeeScreenAnalytics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.impl.R
import com.coffeepeek.feature.coffee.impl.ui.CoffeeDetailViewModel
import com.coffeepeek.feature.coffee.impl.ui.compose.component.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*
import com.coffeepeek.feature.coffee.impl.ui.data.coffeeProductFormLabel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun CoffeeDetailScreen(
    viewModel: CoffeeDetailViewModel,
    imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    galleryContent: @Composable (List<String>, Int, () -> Unit) -> Unit,
    formatCheckedAt: (String) -> String,
    modifier: Modifier,
    screen: CoffeeScreenAnalytics,
    onEvent: (CoffeeDetailEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    BackHandler { viewModel.onAction(if (state.arePhotosOpen) CoffeeDetailAction.ClosePhotos else CoffeeDetailAction.Back) }
    CoffeeDetailScreenContent(state, viewModel::onAction, imageContent, galleryContent, formatCheckedAt, modifier, screen)
}

@Composable
internal fun CoffeeDetailScreenContent(
    state: CoffeeDetailState,
    onAction: (CoffeeDetailAction) -> Unit,
    imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    galleryContent: @Composable (List<String>, Int, () -> Unit) -> Unit,
    formatCheckedAt: (String) -> String,
    modifier: Modifier = Modifier,
    screen: CoffeeScreenAnalytics = CoffeeScreenAnalytics.None,
) {
    screen.TrackDetail(loaded = state.details != null)
    val details = state.details
    val hazeState = rememberHazeState()
    val labels = remember(state.filterGroups) { state.filterGroups.associate { group -> group.code to group.options.associate { it.code to it.name } } }
    if (state.arePhotosOpen && details != null) galleryContent(details.photos, 0) { onAction(CoffeeDetailAction.ClosePhotos) }
    Box(modifier.fillMaxSize()) {
        Scaffold(modifier = Modifier.hazeSource(hazeState), containerColor = MaterialTheme.colorScheme.background) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader(stringResource(R.string.coffee_loading))
                }
                state.loadFailed -> CoffeeLoadError({ onAction(CoffeeDetailAction.Retry) }, Modifier.fillMaxSize().padding(padding))
                details != null -> LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("coffee-detail-list"), contentPadding = PaddingValues(bottom = CoffeePeekDimens.spacing6),
                    verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
                    item(key = "photo") {
                        val photosDescription = stringResource(R.string.coffee_open_photos)
                        Box((if (details.photos.isNotEmpty()) screen.exposure(Modifier, CoffeeExposure.Photos) else Modifier).fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)
                            .semantics { contentDescription = photosDescription }
                            .clickable(enabled = details.photos.isNotEmpty(), role = Role.Button, onClickLabel = stringResource(R.string.coffee_open_photos)) {
                                onAction(CoffeeDetailAction.OpenPhotos)
                            }) {
                            details.photos.firstOrNull()?.let { imageContent(it, details.coffee.name,
                                Modifier.fillMaxSize().padding(top = 56.dp, bottom = CoffeePeekDimens.spacing4), ContentScale.Fit) }
                                ?: CoffeePeekImagePlaceholder(contentDescription = stringResource(R.string.coffee_photo_missing))
                        }
                    }
                    item(key = "intro") {
                        Box(Modifier.padding(horizontal = CoffeePeekDimens.spacing4)) {
                            CoffeeDetailIntro(details, labels, state.isDescriptionExpanded) { onAction(CoffeeDetailAction.ToggleDescription) }
                        }
                    }
                    if (details.tasteDescriptors.isNotEmpty()) item(key = "taste") {
                        Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                            SectionTitle(stringResource(R.string.coffee_taste))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                                details.tasteDescriptors.forEach { CoffeeLabel(it) }
                            }
                        }
                    }
                    item(key = "about") {
                        Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                            SectionTitle(stringResource(R.string.coffee_about))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                                listOf(stringResource(R.string.coffee_country) to details.coffee.countries.joinToString(", ").ifBlank { stringResource(R.string.coffee_unspecified_feminine) },
                                    stringResource(R.string.coffee_form) to (coffeeProductFormLabel(details.productForm) ?: stringResource(R.string.coffee_unspecified_feminine)))
                                    .forEach { (label, value) -> Column(Modifier.widthIn(min = 140.dp), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(value, style = MaterialTheme.typography.bodyMedium)
                                    } }
                            }
                            Text(stringResource(R.string.coffee_roaster), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = screen.exposure(Modifier, CoffeeExposure.Roaster))
                            CoffeeRoasterRow(details.coffee.roasterName, details.coffee.roasterPhotoUrl,
                                if (details.roasterSlug != null) ({ onAction(CoffeeDetailAction.OpenRoaster) }) else null, imageContent)
                        }
                    }
                    item(key = "offers-title") {
                        Column(screen.exposure(Modifier, CoffeeExposure.Offers).padding(horizontal = CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                            SectionTitle(stringResource(R.string.coffee_offers))
                            details.checkedAtUtc?.let { Text(stringResource(R.string.coffee_checked_at, formatCheckedAt(it)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (details.coffee.offers.isEmpty()) Text(stringResource(R.string.coffee_no_offers), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(details.coffee.offers) { index, offer ->
                        Box(Modifier.padding(horizontal = CoffeePeekDimens.spacing4)) {
                            CoffeeOfferCard(offer, labels["brew"].orEmpty(), formatCheckedAt) { onAction(CoffeeDetailAction.OpenSeller(index)) }
                        }
                    }
                    if (state.similarLoading || state.similarFailed || state.similar.isNotEmpty()) item(key = "similar") {
                        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                            Box(screen.exposure(Modifier, CoffeeExposure.SimilarCoffees).padding(horizontal = CoffeePeekDimens.spacing4)) { SectionTitle(stringResource(R.string.coffee_similar)) }
                            when {
                                state.similarLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader(stringResource(R.string.coffee_loading)) }
                                state.similarFailed -> TextButton({ onAction(CoffeeDetailAction.RetrySimilar) }, Modifier.padding(horizontal = CoffeePeekDimens.spacing4)) {
                                    Text(stringResource(R.string.coffee_retry_similar))
                                }
                                else -> LazyRow(contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                                    items(state.similar, key = { it.slug }) { coffee -> CoffeeCard(coffee, labels["taste"].orEmpty(),
                                        { onAction(CoffeeDetailAction.OpenSimilar(coffee.slug)) }, imageContent, Modifier.width(280.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(CoffeePeekDimens.spacing3), horizontalArrangement = Arrangement.SpaceBetween) {
            CoffeePeekGlassIconButton({ onAction(CoffeeDetailAction.Back) }, stringResource(R.string.coffee_back), hazeState = hazeState) {
                Icon(CoffeePeekIcons.Back, null)
            }
            CoffeePeekGlassIconButton({ onAction(CoffeeDetailAction.Share) }, stringResource(R.string.coffee_share), enabled = details != null, hazeState = hazeState) {
                Icon(CoffeePeekIcons.Share, null)
            }
        }
    }
}

@Composable private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
}

@PreviewLightDark @Composable private fun CoffeeDetailPreview() = CoffeePeekTheme {
    CoffeeDetailScreenContent(CoffeeDetailState(details = previewCoffeeDetails(), isLoading = false), {},
        { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) }, { _, _, _ -> }, { it })
}
@PreviewLightDark @Composable private fun CoffeeDetailErrorPreview() = CoffeePeekTheme {
    CoffeeDetailScreenContent(CoffeeDetailState(isLoading = false, loadFailed = true), {}, { _, _, _, _ -> }, { _, _, _ -> }, { it })
}
