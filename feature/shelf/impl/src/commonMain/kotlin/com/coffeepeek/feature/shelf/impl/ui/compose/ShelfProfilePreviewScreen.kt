package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
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
import com.coffeepeek.core.designsystem.component.CoffeePeekShimmer
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.PublicShelfProfileViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.component.PublicShelfBag
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfPublicPhotoFallback
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfLedge
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfLedgeHeight
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfLedgeOverlap
import com.coffeepeek.feature.shelf.impl.ui.compose.component.ShelfPublicPhoto
import com.coffeepeek.feature.shelf.impl.ui.compose.component.SHELF_COLUMNS
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

private val ShelfProfileBagHeight = 136.dp

@Composable
internal fun ShelfProfilePreviewScreen(model: PublicShelfProfileViewModel, repository: ShelfProfileRepository,
    onOpenCoffee: (String) -> Unit, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenCoffee)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(PublicShelfProfileAction.Refresh) }
    LaunchedEffect(model) { model.events.collect { if (it is PublicShelfProfileEvent.OpenCoffee) open(it.slug) } }
    key(state.revision) {
        ShelfProfilePreviewContent(state.shelf, { model.onAction(PublicShelfProfileAction.OpenCoffee(it.id)) }, modifier,
            loading = state.loading) { item, imageModifier ->
            ShelfPublicPhoto(item, repository::photo, imageModifier)
        }
    }
}

@Composable
internal fun ShelfProfilePreviewContent(value: PublicShelf?, onOpen: (PublicShelfItem) -> Unit, modifier: Modifier = Modifier,
    loading: Boolean = false,
    photo: @Composable (PublicShelfItem, Modifier) -> Unit = { _, imageModifier -> ShelfPublicPhotoFallback(imageModifier) }) {
    if (value == null) {
        if (loading) ShelfProfileLoadingContent(modifier)
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.shelf_profile_title), style = MaterialTheme.typography.titleLarge)
        if (value.items.isEmpty()) Text(stringResource(Res.string.shelf_profile_empty),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else ShelfProfileBags(value.items, onOpen, photo)
    }
}

@Composable
private fun ShelfProfileLoadingContent(modifier: Modifier) {
    val loading = stringResource(Res.string.shelf_loading)
    Column(modifier.fillMaxWidth().clearAndSetSemantics {
        contentDescription = loading
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekShimmer(Modifier.width(180.dp).height(28.dp))
        CoffeePeekShimmer(Modifier.fillMaxWidth().height(160.dp))
    }
}

@Composable
private fun ShelfProfileBags(items: List<PublicShelfItem>, onOpen: (PublicShelfItem) -> Unit,
    photo: @Composable (PublicShelfItem, Modifier) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
        items.chunked(SHELF_COLUMNS).forEach { row ->
            ShelfProfileBagRow(row, onOpen, photo)
        }
    }
}

@Composable
private fun ShelfProfileBagRow(items: List<PublicShelfItem>, onOpen: (PublicShelfItem) -> Unit,
    photo: @Composable (PublicShelfItem, Modifier) -> Unit) {
    Box(Modifier.fillMaxWidth().height(ShelfProfileBagHeight + ShelfLedgeHeight - ShelfLedgeOverlap)) {
        ShelfLedge(Modifier.offset(y = ShelfProfileBagHeight - ShelfLedgeOverlap))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(SHELF_COLUMNS) { index ->
                val item = items.getOrNull(index)
                if (item == null) Spacer(Modifier.weight(1f)) else PublicShelfBag(item, { onOpen(item) }, Modifier.weight(1f)) {
                    photo(item, it)
                }
            }
        }
    }
}

@Composable
private fun ShelfProfilePreviewSample(dark: Boolean, bagCount: Int = 3) = CoffeePeekTheme(darkTheme = dark) {
    val bags = listOf(
        PublicShelfItem("one", "Эфиопия", "Sense", "ethiopia", null),
        PublicShelfItem("two", "Бразилия", "Coffee roaster", null, null),
        PublicShelfItem("three", "Колумбия", null, null, null),
        PublicShelfItem("four", "Кения", null, null, null),
        PublicShelfItem("five", "Гватемала", "Coffee roaster", "guatemala", null),
        PublicShelfItem("six", "Руанда", null, "rwanda", null),
        PublicShelfItem("seven", "Коста-Рика", "Sense", "costa-rica", null),
    )
    ShelfProfilePreviewContent(PublicShelf(bags.take(bagCount), bagCount), {})
}

@Preview @Composable private fun ShelfProfilePreviewLight() = ShelfProfilePreviewSample(false)
@Preview @Composable private fun ShelfProfilePreviewDark() = ShelfProfilePreviewSample(true)
@Preview @Composable private fun ShelfProfileSingleBagLight() = ShelfProfilePreviewSample(false, 1)
@Preview @Composable private fun ShelfProfileSingleBagDark() = ShelfProfilePreviewSample(true, 1)
@Preview @Composable private fun ShelfProfileOverflowLight() = ShelfProfilePreviewSample(false, 4)
@Preview @Composable private fun ShelfProfileOverflowDark() = ShelfProfilePreviewSample(true, 4)
@Preview @Composable private fun ShelfProfileMultipleRowsLight() = ShelfProfilePreviewSample(false, 7)
@Preview @Composable private fun ShelfProfileMultipleRowsDark() = ShelfProfilePreviewSample(true, 7)

@Composable
private fun ShelfProfileStatusSample(dark: Boolean, loading: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfProfilePreviewContent(if (loading) null else PublicShelf(emptyList(), 0), {}, loading = loading)
}

@Preview @Composable private fun ShelfProfileLoadingLight() = ShelfProfileStatusSample(false, true)
@Preview @Composable private fun ShelfProfileLoadingDark() = ShelfProfileStatusSample(true, true)
@Preview @Composable private fun ShelfProfileEmptyLight() = ShelfProfileStatusSample(false, false)
@Preview @Composable private fun ShelfProfileEmptyDark() = ShelfProfileStatusSample(true, false)
