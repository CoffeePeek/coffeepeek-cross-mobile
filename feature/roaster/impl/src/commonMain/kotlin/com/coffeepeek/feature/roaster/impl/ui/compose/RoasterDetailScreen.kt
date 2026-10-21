package com.coffeepeek.feature.roaster.impl.ui.compose

import com.coffeepeek.feature.roaster.api.RoasterImage
import com.coffeepeek.feature.roaster.api.RoasterPhotoViewer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.api.RoasterExposure
import com.coffeepeek.feature.roaster.api.RoasterScreenAnalytics
import com.coffeepeek.feature.roaster.domain.model.RoasterAddress
import com.coffeepeek.feature.roaster.domain.model.RoasterContact
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterLocation
import com.coffeepeek.feature.roaster.domain.model.RoasterPhoto
import com.coffeepeek.feature.roaster.domain.model.RoasterShop
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_about_title
import com.coffeepeek.feature.roaster.impl.resources.roaster_address_title
import com.coffeepeek.feature.roaster.impl.resources.roaster_links_title
import com.coffeepeek.feature.roaster.impl.resources.roaster_loading
import com.coffeepeek.feature.roaster.impl.resources.roaster_shops_empty
import com.coffeepeek.feature.roaster.impl.resources.roaster_shops_title
import com.coffeepeek.feature.roaster.impl.ui.RoasterDetailViewModel
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterContactPill
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterDetailError
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterDetailHero
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterSection
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterShopCard
import com.coffeepeek.feature.roaster.impl.ui.compose.component.text
import com.coffeepeek.feature.roaster.impl.ui.data.instagramDisplayText
import com.coffeepeek.feature.roaster.impl.ui.data.prettyExternalLink
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailState

@Composable
internal fun RoasterDetailScreen(
    viewModel: RoasterDetailViewModel,
    image: RoasterImage,
    photoViewer: RoasterPhotoViewer,
    screen: RoasterScreenAnalytics,
    onFavoriteFeedback: () -> Unit,
    modifier: Modifier,
    onEvent: (RoasterDetailEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    RoasterDetailScreenContent(state, viewModel::onAction, image, photoViewer, screen, onFavoriteFeedback, modifier)
}

@Composable
internal fun RoasterDetailScreenContent(
    state: RoasterDetailState,
    onAction: (RoasterDetailAction) -> Unit,
    image: RoasterImage,
    photoViewer: RoasterPhotoViewer,
    screen: RoasterScreenAnalytics = RoasterScreenAnalytics.None,
    onFavoriteFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    screen.TrackDetail(loaded = state.details != null)
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.text()
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message); onAction(RoasterDetailAction.DismissMessage) }
    }
    state.previewPhotoUrl?.let { photoViewer(it) { onAction(RoasterDetailAction.ClosePhoto) } }
    val details = state.details
    // Zero insets: the hero runs edge-to-edge like the legacy screen and pads its own controls.
    Scaffold(modifier, snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) {
        when {
            state.isLoading && details == null -> Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.roaster_loading))
            }
            state.loadFailed && details == null -> RoasterDetailError({ onAction(RoasterDetailAction.Retry) },
                { onAction(RoasterDetailAction.Back) }, Modifier.safeDrawingPadding())
            details != null -> RoasterDetailContent(details, state.isFavorite, onAction, image, screen, onFavoriteFeedback)
        }
    }
}

@Composable
private fun RoasterDetailContent(
    details: RoasterDetails,
    isFavorite: Boolean,
    onAction: (RoasterDetailAction) -> Unit,
    image: RoasterImage,
    screen: RoasterScreenAnalytics,
    onFavoriteFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = CoffeePeekDimens.spacing6)) {
        item {
            RoasterDetailHero(details.name, details.photos, isFavorite,
                onToggleFavorite = { onFavoriteFeedback(); onAction(RoasterDetailAction.ToggleFavorite) },
                onBack = { onAction(RoasterDetailAction.Back) },
                onPhotoClick = { onAction(RoasterDetailAction.OpenPhoto(it)) }, image = image,
                modifier = if (details.photos.isNotEmpty()) screen.exposure(Modifier, RoasterExposure.Photos) else Modifier,
                heartModifier = screen.exposure(Modifier, RoasterExposure.FavoriteDetail))
        }
        item {
            Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
                Text(details.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                details.about?.takeIf { it.isNotBlank() }?.let { about ->
                    RoasterSection(stringResource(Res.string.roaster_about_title)) {
                        Text(about, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                details.location?.let { location ->
                    RoasterSection(stringResource(Res.string.roaster_address_title)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                            Icon(CoffeePeekIcons.Location, null, tint = MaterialTheme.colorScheme.primary)
                            Text(location.address, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                val instagram = details.contact?.instagramLink
                val website = details.contact?.siteLink
                if (!instagram.isNullOrBlank() || !website.isNullOrBlank()) RoasterSection(stringResource(Res.string.roaster_links_title)) {
                    Column(screen.exposure(Modifier.fillMaxWidth(), RoasterExposure.Contacts), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        if (!instagram.isNullOrBlank()) RoasterContactPill(CoffeePeekIcons.Instagram, instagramDisplayText(instagram)) {
                            onAction(RoasterDetailAction.OpenInstagram)
                        }
                        if (!website.isNullOrBlank()) RoasterContactPill(CoffeePeekIcons.Globe, prettyExternalLink(website)) {
                            onAction(RoasterDetailAction.OpenWebsite)
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(Res.string.roaster_shops_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = screen.exposure(Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2), RoasterExposure.Shops))
        }
        if (details.shops.isEmpty()) item {
            Text(stringResource(Res.string.roaster_shops_empty), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3))
        } else items(details.shops, key = { it.slug }) { shop ->
            RoasterShopCard(shop, { onAction(RoasterDetailAction.OpenShop(shop.slug)) }, image)
        }
    }
}

private fun previewDetails(shops: List<RoasterShop> = emptyList()) = RoasterDetails(
    id = "sandbox", address = RoasterAddress("sandbox", "/roasters/sandbox", 1, false), name = "Sandbox Roaster",
    about = "Локальный обжарщик для тестирования", location = RoasterLocation("Минск, Тестовая улица, 1", null, null),
    contact = RoasterContact("@sandbox.roaster", "sandbox.example/roaster"),
    photos = listOf(RoasterPhoto("1", "https://x/1", 1), RoasterPhoto("2", "https://x/2", 2)), shops = shops)

private val previewShops = listOf(RoasterShop("cafe", "Sandbox Cafe", null, RoasterAddress("cafe", "/coffee-shops/cafe", 1, false)))

@Preview @Composable private fun RoasterDetailLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailScreenContent(RoasterDetailState(isLoading = false, details = previewDetails(previewShops), favoriteKeys = setOf("sandbox")),
        {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailScreenContent(RoasterDetailState(isLoading = false, details = previewDetails(previewShops), favoriteKeys = setOf("sandbox")),
        {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailNoShopsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailScreenContent(RoasterDetailState(isLoading = false, details = previewDetails().copy(contact = null, location = null, about = null, photos = emptyList())),
        {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailNoShopsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailScreenContent(RoasterDetailState(isLoading = false, details = previewDetails().copy(contact = null, location = null, about = null, photos = emptyList())),
        {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailScreenContent(RoasterDetailState(), {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailScreenContent(RoasterDetailState(), {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailFailedLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterDetailScreenContent(RoasterDetailState(isLoading = false, loadFailed = true), {}, { _, _, _, _, _ -> }, { _, _ -> })
}

@Preview @Composable private fun RoasterDetailFailedDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterDetailScreenContent(RoasterDetailState(isLoading = false, loadFailed = true), {}, { _, _, _, _, _ -> }, { _, _ -> })
}
