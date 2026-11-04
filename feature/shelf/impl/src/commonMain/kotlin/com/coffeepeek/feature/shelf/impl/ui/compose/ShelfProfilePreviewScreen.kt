package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.PublicShelfProfileViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.component.PublicShelfBag
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfBagPlaceholder
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfLedge
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfLedgeOverlap
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfPublicPhoto
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun ShelfProfilePreviewScreen(model: PublicShelfProfileViewModel, repository: ShelfProfileRepository,
    onOpenCoffee: (String) -> Unit, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenCoffee)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(PublicShelfProfileAction.Refresh) }
    LaunchedEffect(model) { model.events.collect { if (it is PublicShelfProfileEvent.OpenCoffee) open(it.slug) } }
    key(state.revision) {
        ShelfProfilePreviewContent(state.shelf, { model.onAction(PublicShelfProfileAction.OpenCoffee(it.id)) }, modifier) { item, imageModifier ->
            ShelfPublicPhoto(item, repository::photo, imageModifier)
        }
    }
}

@Composable
internal fun ShelfProfilePreviewContent(value: PublicShelf?, onOpen: (PublicShelfItem) -> Unit, modifier: Modifier = Modifier,
    photo: @Composable (PublicShelfItem, Modifier) -> Unit = { item, imageModifier ->
        ShelfBagPlaceholder(item.name.takeIf(String::isNotBlank) ?: stringResource(Res.string.shelf_bag_untitled), imageModifier) }) {
    if (value == null) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.shelf_profile_title), style = MaterialTheme.typography.titleLarge)
        Box(Modifier.fillMaxWidth().heightIn(min = 168.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { index ->
                    val item = value.items.getOrNull(index)
                    if (item == null) Spacer(Modifier.weight(1f)) else PublicShelfBag(
                        item, { onOpen(item) }, Modifier.weight(1f), onShelf = true,
                        photo = { photo(item, it) },
                    )
                }
            }
            ShelfLedge(Modifier.offset(y = 136.dp - ShelfLedgeOverlap))
        }
        if (value.items.isEmpty()) Text(stringResource(Res.string.shelf_profile_empty),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShelfProfilePreviewSample(dark: Boolean, bagCount: Int = 3) = CoffeePeekTheme(darkTheme = dark) {
    val bags = listOf(
        PublicShelfItem("one", "Эфиопия", "Sense", "ethiopia", null),
        PublicShelfItem("two", "Бразилия", "Coffee roaster", null, null),
        PublicShelfItem("three", "Колумбия", null, null, null),
        PublicShelfItem("four", "Кения", null, null, null),
    )
    ShelfProfilePreviewContent(PublicShelf(bags.take(bagCount), bagCount), {})
}

@Preview @Composable private fun ShelfProfilePreviewLight() = ShelfProfilePreviewSample(false)
@Preview @Composable private fun ShelfProfilePreviewDark() = ShelfProfilePreviewSample(true)
@Preview @Composable private fun ShelfProfileSingleBagLight() = ShelfProfilePreviewSample(false, 1)
@Preview @Composable private fun ShelfProfileSingleBagDark() = ShelfProfilePreviewSample(true, 1)
@Preview @Composable private fun ShelfProfileOverflowLight() = ShelfProfilePreviewSample(false, 4)
@Preview @Composable private fun ShelfProfileOverflowDark() = ShelfProfilePreviewSample(true, 4)
