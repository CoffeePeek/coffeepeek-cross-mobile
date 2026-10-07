package com.coffeepeek.admin.feature.coffee.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.coffee.domain.Coffee
import com.coffeepeek.admin.feature.coffee.domain.CoffeeOffer
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.PriceBynIcon
import com.coffeepeek.admin.ui.component.SearchHeader
import kotlin.math.round
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CoffeeListScreen(vm: CoffeeListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val clearance = LocalFloatingNavClearance.current
    val tasteNames = state.filterGroups.firstOrNull { it.code == "taste" }?.options.orEmpty().associate { it.code to it.name }

    LaunchedEffect(state.query, state.filters) {
        if (state.isLoading) listState.scrollToItem(0)
    }
    LaunchedEffect(state.items.size, state.hasMore, state.isLoading, state.isLoadingMore, state.error) {
        if (state.items.isNotEmpty() && state.hasMore && !state.isLoading && !state.isLoadingMore && state.error == null) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { index ->
                if (index >= state.items.size - 4) vm.loadMore()
            }
        }
    }

    if (showFilters) CoffeeFiltersSheet(
        state = state,
        onDismiss = { showFilters = false },
        onApply = { vm.applyFilters(it); showFilters = false },
        onRetry = vm::loadFilters,
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3),
            ) {
                SearchHeader(
                    query = state.query,
                    onQueryChange = vm::onQueryChange,
                    placeholder = "Название кофе",
                    showCategories = false,
                    filterCount = state.filters.activeCount,
                    onFilters = { showFilters = true },
                )
            }
        },
    ) { padding ->
        CoffeePeekPullToRefresh(
            listState = listState,
            isRefreshing = state.isLoading,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
        ) { scrollModifier ->
            LazyColumn(
                state = listState,
                modifier = scrollModifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = CpDimens.spacing4, top = CpDimens.spacing4,
                    end = CpDimens.spacing4, bottom = clearance + CpDimens.spacing4,
                ),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
            ) {
                items(state.items, key = { it.slug }) { coffee -> CoffeeCard(coffee, tasteNames) }
                if (state.isLoadingMore) item {
                    Box(Modifier.fillMaxWidth().padding(CpDimens.spacing6), contentAlignment = Alignment.Center) {
                        CoffeePeekLoader()
                    }
                }
                if (state.error != null) item {
                    Column(Modifier.fillMaxWidth().padding(CpDimens.spacing4), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { if (state.items.isEmpty()) vm.refresh() else vm.loadMore() }) {
                            Text("Попробовать снова")
                        }
                    }
                } else if (state.items.isEmpty() && !state.isLoading) item {
                    Column(Modifier.fillMaxWidth().padding(CpDimens.spacing6), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Кофе не найден", style = MaterialTheme.typography.titleMedium)
                        Text("Попробуйте изменить поиск или фильтры", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CoffeeCard(coffee: Coffee, tasteNames: Map<String, String>, modifier: Modifier = Modifier) {
    val offers = remember(coffee.offers) {
        coffee.offers.distinctBy { it.weightGrams }.sortedBy { it.weightGrams ?: Int.MAX_VALUE }
    }
    var selectedWeight by rememberSaveable(coffee.slug) {
        mutableStateOf(coffee.offers.firstOrNull()?.weightGrams)
    }
    val offer = offers.firstOrNull { it.weightGrams == selectedWeight } ?: offers.firstOrNull()
    Card(
        onClick = { Navigator.navigate(Navigator.Screen.CoffeeDetail(coffee.slug)) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radius4xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(216.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val photo = coffee.photoUrl
            if (!photo.isNullOrBlank()) CoffeeShopImage(
                imageUrl = photo, contentDescription = coffee.name,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
            ) else CoffeeShopPlaceholderImage(contentDescription = "Фото ${coffee.name} отсутствует")
            Box(
                Modifier.align(Alignment.BottomEnd).padding(CpDimens.spacing4).size(48.dp)
                    .shadow(3.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    .semantics { contentDescription = coffee.roasterName },
                contentAlignment = Alignment.Center,
            ) {
                val logo = coffee.roasterPhotoUrl
                if (!logo.isNullOrBlank()) CoffeeShopImage(
                    imageUrl = logo, contentDescription = coffee.roasterName,
                    contentScale = ContentScale.Fit, placeholderLabelSize = MaterialTheme.typography.labelSmall.fontSize,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                ) else Text(
                    coffee.roasterName.take(2).uppercase(), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text(coffee.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (coffee.countries.isNotEmpty()) Text(
                coffee.countries.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tastes = coffee.tasteCodes.mapNotNull(tasteNames::get)
            if (tastes.isNotEmpty()) FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
            ) { tastes.forEach { CoffeeLabel(it) } }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                CoffeePrice(offer)
                if (offers.size > 1 && offer != null) CapsuleSegmentedControl(
                    options = offers,
                    selected = offer,
                    label = { it.weightGrams?.let { weight -> "$weight г" } ?: "Не указан" },
                    onSelected = { selectedWeight = it.weightGrams },
                    modifier = Modifier.width((offers.size * 56).dp),
                ) else CoffeeLabel(offer?.weightGrams?.let { "$it г" } ?: "Вес не указан")
            }
        }
    }
}

@Composable
internal fun CoffeePrice(offer: CoffeeOffer?) {
    val price = offer?.price
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
        Text(
            price?.let(::formatCoffeePrice) ?: "Цена не указана",
            style = if (price != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (price != null) FontWeight.Bold else FontWeight.Normal,
        )
        if (price != null) {
            if (offer.currency == "BYN") PriceBynIcon(
                modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = "белорусских рублей",
            ) else Text(offer.currency ?: "", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun formatCoffeePrice(price: Double): String {
    val cents = round(price * 100).toLong()
    return if (cents % 100 == 0L) "${cents / 100}"
    else "${cents / 100}.${(cents % 100).toString().padStart(2, '0').trimEnd('0')}"
}

@Composable
internal fun CoffeeLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing1),
    )
}
