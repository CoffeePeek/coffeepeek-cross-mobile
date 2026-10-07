package com.coffeepeek.admin.feature.coffee.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.coffee.domain.CoffeeAvailability
import com.coffeepeek.admin.feature.coffee.domain.CoffeeOffer
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun CoffeeDetailScreen(slug: String) {
    val vm: CoffeeDetailViewModel = koinViewModel(parameters = { parametersOf(slug) })
    val state by vm.state.collectAsState()
    val details = state.details
    val hazeState = rememberHazeState()
    var showPhotos by rememberSaveable(slug) { mutableStateOf(false) }
    var expandedDescription by rememberSaveable(slug) { mutableStateOf(false) }
    var descriptionOverflows by remember(slug) { mutableStateOf(false) }
    val labels = state.filterGroups.associate { group -> group.code to group.options.associate { it.code to it.name } }
    val tasteNames = labels["taste"].orEmpty()
    val traitWidth = 168.dp * LocalDensity.current.fontScale
    val actionColors = ButtonDefaults.textButtonColors(
        contentColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) CpColor.AccentTextDark else CpColor.AccentTextLight,
    )

    if (showPhotos && details != null) FullScreenImageDialog(
        imageUrls = details.photos, initialIndex = 0, onDismiss = { showPhotos = false },
    )

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.hazeSource(hazeState),
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                state.error != null -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(CpDimens.spacing6),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = vm::load, colors = actionColors) { Text("Попробовать снова") }
                }
                details != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = CpDimens.spacing6),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
                ) {
                    item(key = "photo") {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = details.photos.isNotEmpty(), role = Role.Button, onClickLabel = "Открыть фото кофе") { showPhotos = true },
                        ) {
                            val photo = details.photos.firstOrNull()
                            if (photo != null) CoffeeShopImage(
                                imageUrl = photo, contentDescription = details.coffee.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(top = 56.dp, bottom = CpDimens.spacing4),
                            ) else CoffeeShopPlaceholderImage(contentDescription = "Фото кофе отсутствует")
                        }
                    }
                    item(key = "intro") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            Text(details.coffee.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            val kind = productKindLabel(details.productKind)
                            val form = productFormLabel(details.productForm)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                                details.coffee.countries.forEach { CoffeeLabel(it) }
                                listOfNotNull(kind, form).takeIf { it.isNotEmpty() }?.let { CoffeeLabel(it.joinToString(" · ")) }
                            }
                            details.description?.let { description ->
                                Column {
                                    Text(
                                        description, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (expandedDescription) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { if (!expandedDescription) descriptionOverflows = it.hasVisualOverflow },
                                    )
                                    if (descriptionOverflows || expandedDescription) TextButton(onClick = { expandedDescription = !expandedDescription }, colors = actionColors) {
                                        Text(if (expandedDescription) "Свернуть" else "Читать полностью")
                                    }
                                }
                            }
                            val traits = listOf(
                                Triple("Обжарка", labels["roast"]?.get(details.roastLevel), CpIcons.Fire),
                                Triple("Преобладающий профиль", details.coffee.tasteCodes.mapNotNull(tasteNames::get).takeIf { it.isNotEmpty() }?.joinToString(", "), CpIcons.Sparkle),
                                Triple("Кислотность", labels["acidity"]?.get(details.acidity), CpIcons.Drop),
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
                                traits.forEach { (label, value, icon) ->
                                    if (value != null) Row(Modifier.width(traitWidth), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                                        SettingsIconBadge(icon, SettingsIconColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface))
                                        Column(Modifier.weight(1f)) {
                                            Text(value, style = MaterialTheme.typography.titleSmall)
                                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (details.tasteDescriptors.isNotEmpty()) item(key = "taste") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            SectionTitle("Вкусовой профиль")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                                details.tasteDescriptors.forEach { CoffeeLabel(it) }
                            }
                        }
                    }
                    item(key = "about") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            SectionTitle("О кофе")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing6), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                listOf(
                                    "Страна" to details.coffee.countries.joinToString(", ").ifBlank { "Не указана" },
                                    "Форма" to (productFormLabel(details.productForm) ?: "Не указана"),
                                ).forEach { (label, value) ->
                                    Column(Modifier.widthIn(min = 140.dp), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(value, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            Text("Обжарщик", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedContentCard(contentPadding = PaddingValues(0.dp)) {
                                RoasterLinkRow(
                                    name = details.coffee.roasterName, photoUrl = details.coffee.roasterPhotoUrl,
                                    onClick = details.roasterSlug?.let { roaster -> { Navigator.navigate(Navigator.Screen.RoasterDetail(roaster)) } },
                                )
                            }
                        }
                    }
                    item(key = "offers-title") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                            SectionTitle("Варианты покупки")
                            details.checkedAtUtc?.let { Text("Проверено ${utcIsoToLocalDateTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (details.coffee.offers.isEmpty()) Text("Предложений пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(details.coffee.offers) { _, offer ->
                        Box(Modifier.padding(horizontal = CpDimens.spacing4)) { CoffeeOfferCard(offer, labels["brew"].orEmpty()) }
                    }
                    if (state.similarLoading || state.similarError || state.similar.isNotEmpty()) item(key = "similar") {
                        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            Box(Modifier.padding(horizontal = CpDimens.spacing4)) { SectionTitle("Похожие сорта") }
                            when {
                                state.similarLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                                state.similarError -> TextButton(onClick = vm::loadSimilar, modifier = Modifier.padding(horizontal = CpDimens.spacing4), colors = actionColors) { Text("Загрузить похожие сорта") }
                                else -> LazyRow(contentPadding = PaddingValues(horizontal = CpDimens.spacing4), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                    items(state.similar, key = { it.slug }) { CoffeeCard(it, tasteNames, Modifier.width(280.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(CpDimens.spacing3),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PlatformGlassIconButton(GlassControlIcon.Back, Navigator::popBack, "Назад", hazeState = hazeState) {
                Icon(CpIcons.Back, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            PlatformGlassIconButton(
                GlassControlIcon.Share,
                onClick = { details?.let { ShareHelper.shareText("${it.coffee.name} — https://coffeepeek.by${it.canonicalPath}") } },
                contentDescription = "Поделиться кофе", enabled = details != null, hazeState = hazeState,
            ) {
                Icon(CpIcons.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CoffeeOfferCard(offer: CoffeeOffer, brewNames: Map<String, String>) {
    OutlinedContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
            val (availability, color) = when (offer.availability) {
                CoffeeAvailability.InStock -> "В наличии" to CpColor.Success
                CoffeeAvailability.OutOfStock -> "Нет в наличии" to CpColor.Error
                CoffeeAvailability.PreOrder -> "Предзаказ" to CpColor.Warning
                CoffeeAvailability.Unknown -> "Наличие не подтверждено" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(offer.weightGrams?.let { "$it г" } ?: "Вес не указан", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(availability, style = MaterialTheme.typography.bodySmall)
                }
            }
            CoffeePrice(offer)
            AppButton("Перейти к продавцу", onClick = { offer.sourceUrl?.let(OpenInBrowser::openInBrowser) }, enabled = offer.sourceUrl != null)
            val fields = listOf(
                "Продавец" to (offer.sellerName ?: "Не указан"),
                "Помол" to (productFormLabel(offer.grind) ?: "Не указан"),
                "Приготовление" to (brewNames[offer.brewPurpose] ?: "Не подтверждено"),
                "Источник" to when (offer.availabilityScope) { "online" -> "Онлайн"; "offline" -> "В магазине"; "both" -> "Онлайн и в магазине"; else -> "Не указан" },
            ) + listOfNotNull(offer.checkedAtUtc?.let { "Проверено" to utcIsoToLocalDateTime(it) })
            fields.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                }
            }
        }
    }
}

private fun productKindLabel(code: String?): String? = when (code) {
    "roasted_beans" -> "Обжаренный кофе"
    "green_beans" -> "Зелёный кофе"
    else -> null
}

private fun productFormLabel(code: String?): String? = when (code) {
    "whole_beans" -> "В зёрнах"
    "ground" -> "Молотый"
    "capsules" -> "Капсулы"
    "drip_bags" -> "Дрип-пакеты"
    else -> null
}
