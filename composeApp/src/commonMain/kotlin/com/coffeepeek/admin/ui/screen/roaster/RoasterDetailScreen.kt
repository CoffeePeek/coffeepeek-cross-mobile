package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.ui.component.CpCircularBackButton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.FavoriteButton
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterShop
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoasterDetailScreen(roasterId: String) {
    val vm: RoasterDetailViewModel = platformViewModel(parameters = { parametersOf(roasterId) })
    val state by vm.state.collectAsState()
    var previewUrl by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionMessage()
        }
    }

    previewUrl?.let { url ->
        FullScreenImageDialog(imageUrl = url, onDismiss = { previewUrl = null })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CoffeePeekLoader() }

            state.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(CpDimens.spacing4),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                ) {
                    Text(
                        text = state.error ?: "Ошибка загрузки",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = vm::load,
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                    ) { Text("Повторить") }
                    OutlinedButton(
                        onClick = Navigator::popBack,
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                    ) { Text("Назад") }
                }
            }

            state.details != null -> RoasterContent(
                details = state.details!!,
                isFavorite = state.isFavorite,
                isFavoriteLoading = state.isFavoriteLoading,
                onToggleFavorite = vm::toggleFavorite,
                onBack = Navigator::popBack,
                onPhotoClick = { previewUrl = it },
                onShopClick = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            )
        }
    }
}

@Composable
private fun RoasterContent(
    details: RoasterDetails,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = CpDimens.spacing6),
    ) {
        item {
            RoasterHero(
                details = details,
                isFavorite = isFavorite,
                isFavoriteLoading = isFavoriteLoading,
                onToggleFavorite = onToggleFavorite,
                onBack = onBack,
                onPhotoClick = onPhotoClick,
            )
        }
        item {
            Column(
                modifier = Modifier.padding(CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
            ) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )

                details.about?.takeIf { it.isNotBlank() }?.let { about ->
                    RoasterSection(title = "Об обжарщике") {
                        Text(
                            text = about,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                details.location?.let { location ->
                    RoasterSection(title = "Адрес") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                        ) {
                            Icon(
                                CpIcons.Location,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(location.address, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                val instagram = details.contact?.instagramLink
                val website = details.contact?.siteLink
                if (!instagram.isNullOrBlank() || !website.isNullOrBlank()) {
                    RoasterSection(title = "Ссылки") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            if (!instagram.isNullOrBlank()) {
                                RoasterContactPill(
                                    icon = CpIcons.Instagram,
                                    text = instagramDisplayText(instagram),
                                    onClick = { OpenInBrowser.openInBrowser(normalizeInstagramUrl(instagram)) },
                                )
                            }
                            if (!website.isNullOrBlank()) {
                                RoasterContactPill(
                                    icon = CpIcons.Globe,
                                    text = prettyExternalLink(website),
                                    onClick = { OpenInBrowser.openInBrowser(normalizeExternalUrl(website)) },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Где используют",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
            )
        }

        if (details.shops.isEmpty()) {
            item {
                Text(
                    text = "Пока нет кофеен, которые указали этого обжарщика.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3),
                )
            }
        } else {
            items(details.shops, key = { it.id }) { shop ->
                RoasterShopCard(shop = shop, onClick = { onShopClick(shop.id) })
            }
        }
    }
}

@Composable
private fun RoasterShopCard(
    shop: RoasterShop,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing1)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CpDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CpDimens.spacing2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(CpDimens.radiusMd)),
            ) {
                val photoUrl = shop.photoUrl?.takeIf(String::isNotBlank)
                if (photoUrl != null) {
                    CoffeeShopImage(
                        imageUrl = photoUrl,
                        contentDescription = "Фото кофейни ${shop.name}",
                        placeholderLabelSize = 9.sp,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CoffeeShopPlaceholderImage(
                        labelSize = 9.sp,
                        contentDescription = "Фото кофейни ${shop.name} отсутствует",
                    )
                }
            }
            Text(
                text = shop.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = CpDimens.spacing3),
            )
            Icon(
                imageVector = CpIcons.ChevronRight,
                contentDescription = "Открыть кофейню ${shop.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoasterHero(
    details: RoasterDetails,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (details.photos.isEmpty()) {
            CoffeeShopPlaceholderImage(
                labelSize = 24.sp,
                contentDescription = "Фотографии ${details.name} отсутствуют",
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { details.photos.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val photo = details.photos[page]
                CoffeeShopImage(
                    imageUrl = photo.fullUrl,
                    contentDescription = details.name,
                    contentScale = ContentScale.Crop,
                    placeholderLabelSize = 24.sp,
                    modifier = Modifier.fillMaxSize().clickable { onPhotoClick(photo.fullUrl) },
                )
            }
            if (details.photos.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(CpDimens.spacing3),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${details.photos.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing1),
                    )
                }
            }
        }

        CpCircularBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(CpDimens.spacing2),
        )
        FavoriteButton(
            isFavorite = isFavorite,
            onClick = onToggleFavorite,
            enabled = !isFavoriteLoading,
            overImage = true,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(CpDimens.spacing3),
        )
    }
}

@Composable
private fun RoasterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun RoasterContactPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun normalizeInstagramUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    val handle = trimmed.removePrefix("@").trim('/')
    return "https://instagram.com/$handle"
}

private fun instagramDisplayText(value: String): String {
    val normalized = normalizeInstagramUrl(value)
    val handle = normalized
        .substringAfter("instagram.com/", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .trim()
    return if (handle.isNotBlank()) "@$handle" else prettyExternalLink(value)
}

private fun prettyExternalLink(value: String): String = normalizeExternalUrl(value)
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore('?')
    .substringBefore('#')
    .trimEnd('/')

private fun normalizeExternalUrl(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}
