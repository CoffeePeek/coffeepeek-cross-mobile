package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInBeanOption
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInShelfBag

@Composable
internal fun HomeCheckInBeanPicker(
    state: HomeCheckInEditorState, onAction: (HomeCheckInEditorAction) -> Unit,
    emptyArtwork: @Composable (Modifier) -> Unit, bagImage: @Composable (url: String, Modifier) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var customName by rememberSaveable { mutableStateOf(state.draft.customBeanName.orEmpty()) }
    val bags = state.shelfBags.matching(state.beanQuery)
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAction(HomeCheckInEditorAction.CloseBeanPicker) }, Modifier.size(44.dp)) {
                Icon(CoffeePeekIcons.Back, stringResource(R.string.home_check_in_back_to_editor))
            }
            Text(stringResource(R.string.home_check_in_bean), style = MaterialTheme.typography.headlineSmall)
        }
        CoffeePeekSearchField(state.beanQuery, { onAction(HomeCheckInEditorAction.BeanQueryChanged(it)) },
            stringResource(R.string.home_check_in_bean_search), stringResource(R.string.home_check_in_clear_search),
            modifier = Modifier.fillMaxWidth(), onSearch = { onAction(HomeCheckInEditorAction.SearchBeans) })
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                bags.isNotEmpty() || state.beans.isNotEmpty() -> HomeCheckInBeanResults(bags, state, bagImage, onAction)
                state.beansLoading -> CoffeePeekLoader(stringResource(R.string.home_check_in_beans_loading))
                state.beansIssue != null -> HomeCheckInBeansFailed(onAction)
                else -> HomeCheckInBeansEmpty(emptyArtwork)
            }
        }
        HomeCheckInCustomBean(customName, { customName = it }, onAction)
    }
}

/** The owner's bags first, then the catalogue; a catalogue search still loading or failing keeps the bags usable. */
@Composable
private fun HomeCheckInBeanResults(
    bags: List<HomeCheckInShelfBag>, state: HomeCheckInEditorState, bagImage: @Composable (url: String, Modifier) -> Unit,
    onAction: (HomeCheckInEditorAction) -> Unit,
) {
    val unnamed = stringResource(R.string.home_check_in_shelf_bag_unnamed)
    val shelfTitle = stringResource(R.string.home_check_in_from_shelf)
    val catalogTitle = stringResource(R.string.home_check_in_from_catalog)
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bags.isNotEmpty()) {
            section(shelfTitle)
            items(bags, key = { "bag-${it.id}" }) { bag ->
                HomeCheckInShelfBagRow(bag, unnamed, bagImage) { onAction(HomeCheckInEditorAction.ShelfBagSelected(bag.id)) }
            }
            if (state.beans.isNotEmpty() || state.beansLoading || state.beansIssue != null) section(catalogTitle)
        }
        items(state.beans, key = { "coffee-${it.slug}" }) { bean ->
            HomeCheckInBeanRow(bean.name, bean.roasterName) { onAction(HomeCheckInEditorAction.BeanSelected(bean.slug)) }
        }
        if (state.beans.isEmpty() && state.beansLoading) item(key = "loading") {
            CoffeePeekLoader(stringResource(R.string.home_check_in_beans_loading), Modifier.fillMaxWidth())
        }
        if (state.beans.isEmpty() && state.beansIssue != null) item(key = "failed") { HomeCheckInBeansFailed(onAction) }
    }
}

private fun LazyListScope.section(title: String) = item(key = "section-$title") {
    Text(title, Modifier.padding(top = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun HomeCheckInBeanRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

/** A bag with its card on the left, as it stands on the shelf; a bag without a card shows a bean mark instead. */
@Composable
private fun HomeCheckInShelfBagRow(
    bag: HomeCheckInShelfBag, unnamed: String, image: @Composable (url: String, Modifier) -> Unit, onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 44.dp, height = 56.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            bag.imageUrl?.let { image(it, Modifier.fillMaxSize()) }
                ?: Icon(CoffeePeekIcons.CoffeeBean, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(bag.name ?: unnamed, style = MaterialTheme.typography.bodyLarge)
            bag.roasterName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun HomeCheckInBeansFailed(onAction: (HomeCheckInEditorAction) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_check_in_beans_failed), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { onAction(HomeCheckInEditorAction.SearchBeans) }) { Text(stringResource(R.string.home_check_in_retry)) }
    }
}

@Composable
private fun HomeCheckInBeansEmpty(emptyArtwork: @Composable (Modifier) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        emptyArtwork(Modifier.size(120.dp))
        Text(stringResource(R.string.home_check_in_beans_empty), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.home_check_in_beans_empty_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeCheckInCustomBean(name: String, onName: (String) -> Unit, onAction: (HomeCheckInEditorAction) -> Unit) {
    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeCheckInEditorField(stringResource(R.string.home_check_in_custom_bean)) {
            HomeCheckInEditorInput(name, onName, stringResource(R.string.home_check_in_custom_bean_name),
                stringResource(R.string.home_check_in_custom_bean_hint), true, maxLength = 250)
        }
        CoffeePeekButton(stringResource(R.string.home_check_in_use_custom_bean),
            { onAction(HomeCheckInEditorAction.CustomBeanChosen(name.trim())) }, enabled = name.isNotBlank())
    }
}

/** Bags are few, so the query filters them here instead of asking the server. */
private fun List<HomeCheckInShelfBag>.matching(query: String): List<HomeCheckInShelfBag> {
    val text = query.trim().takeIf(String::isNotEmpty) ?: return this
    return filter { bag -> listOfNotNull(bag.name, bag.roasterName).any { it.contains(text, ignoreCase = true) } }
}

@PreviewLightDark @Composable private fun HomeCheckInBeanPickerPreview() = CoffeePeekTheme {
    Surface { HomeCheckInBeanPicker(HomeCheckInEditorState(isBeanPickerOpen = true), {},
        { Icon(CoffeePeekIcons.Search, null, it) }) }
}

@PreviewLightDark @Composable private fun HomeCheckInBeanPickerShelfPreview() = CoffeePeekTheme {
    Surface { HomeCheckInBeanPicker(HomeCheckInEditorState(isBeanPickerOpen = true,
        shelfBags = listOf(HomeCheckInShelfBag("1", "Эфиопия Иргачеффе", "Sibaristica", "https://preview.invalid/card"), HomeCheckInShelfBag("2", null, null)),
        beans = listOf(HomeCheckInBeanOption("honey-bloom", "Honey Bloom", "Tasty Coffee"))), {},
        { Icon(CoffeePeekIcons.Search, null, it) }, { _, modifier -> Box(modifier.background(MaterialTheme.colorScheme.tertiary)) }) }
}
