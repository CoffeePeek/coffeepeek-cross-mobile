package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionsPicker(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    modifier: Modifier = Modifier, image: @Composable (SavedCollectionSummary, Modifier) -> Unit = { _, m -> CoffeePeekImagePlaceholder(m) }) {
    if (state.quickTitle != null) {
        SavedQuickCreate(state, action)
        return
    }
    val maximumHeight = savedPickerMaximumHeight()
    CoffeePeekSwipeDismissModalBottomSheet({ action(SavedCollectionsAction.Back) }, stringResource(Res.string.collections_cancel),
        modifier) {
        SavedPickerContent(state, action, maximumHeight, image)
    }
}

@Composable
private fun SavedPickerContent(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, maximumHeight: Dp,
    image: @Composable (SavedCollectionSummary, Modifier) -> Unit = { _, m -> CoffeePeekImagePlaceholder(m) }) {
    Column(Modifier.fillMaxWidth().heightIn(max = maximumHeight)) {
        SavedPickerToolbar(state, action)
        LazyColumn(Modifier.weight(1f, fill = false).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SavedCollectionsStatus(state, action) }
            item { SavedCollectionsSearch(state, action) }
            SavedCollectionVisibility.entries.reversed().forEach { visibility ->
                val lists = state.lists.filter { it.visibility == visibility && it.title.contains(state.query, true) }
                if (lists.isNotEmpty()) item { SavedCollectionSectionTitle(visibility) }
                items(lists, key = { it.id }) { value -> SavedMembershipRow(value, value.id in state.memberships, !state.busy && state.membershipsLoaded,
                    { action(SavedCollectionsAction.ToggleMembership(value.id)) }) { image(value, it) } }
            }
            if (!state.loading && state.failure != SavedCollectionsFailure.Load && state.lists.isEmpty()) item { Text(stringResource(Res.string.collections_library_empty)) }
            if (state.lists.isNotEmpty() && state.lists.none { it.title.contains(state.query, true) })
                item { Text(stringResource(Res.string.collections_no_matches)) }
        }
        CoffeePeekButton(stringResource(Res.string.collections_done), { action(SavedCollectionsAction.CommitMemberships) },
            enabled = state.signedIn && !state.loading && !state.busy && state.membershipsLoaded,
            modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun SavedPickerToolbar(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CoffeePeekPlainIconButton({ action(SavedCollectionsAction.Back) }) { CoffeePeekIcon(CoffeePeekIcons.Close, stringResource(Res.string.collections_cancel)) }
        Text(stringResource(Res.string.collections_save_to_list), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        CoffeePeekPlainIconButton({ action(SavedCollectionsAction.QuickTitle("")) },
            enabled = state.signedIn && !state.busy && !state.loading && state.membershipsLoaded) {
            CoffeePeekIcon(CoffeePeekIcons.Add, stringResource(Res.string.collections_new_list))
        }
    }
}

@Composable
internal fun SavedMembershipRow(value: SavedCollectionSummary, checked: Boolean, enabled: Boolean, onClick: () -> Unit,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    Column {
        Row(Modifier.fillMaxWidth().toggleable(checked, enabled, role = Role.Checkbox, onValueChange = { onClick() }).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            image(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f)) {
                Text(value.title, style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(Res.string.collections_saved_count, value.itemCount), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(24.dp)) { if (checked) CoffeePeekIcon(CoffeePeekIcons.Check, null, tint = MaterialTheme.colorScheme.primary) }
        }
        CoffeePeekRowSeparator()
    }
}

@Composable
private fun SavedQuickCreate(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    val maximumHeight = savedPickerMaximumHeight()
    CoffeePeekSwipeDismissModalBottomSheet({ action(SavedCollectionsAction.QuickTitle(null)) }, stringResource(Res.string.collections_cancel),
        dismissEnabled = !state.busy) {
        SavedQuickCreateContent(state, action, maximumHeight)
    }
}

@Composable
private fun SavedQuickCreateContent(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, maximumHeight: Dp) {
    Column(Modifier.fillMaxWidth().heightIn(max = maximumHeight).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.collections_new_list), style = MaterialTheme.typography.titleLarge)
        SavedCollectionsStatus(state, action)
        CoffeePeekTextField(stringResource(Res.string.collections_name), state.quickTitle.orEmpty(),
            { action(SavedCollectionsAction.QuickTitle(it)) }, stringResource(Res.string.collections_name), enabled = !state.busy)
        Text(stringResource(Res.string.collections_private_hint), style = MaterialTheme.typography.bodySmall)
        CoffeePeekButton(stringResource(Res.string.collections_create), { action(SavedCollectionsAction.CreateQuick) },
            enabled = !state.busy && !state.loading && state.membershipsLoaded && CollectionRules.validTitle(state.quickTitle.orEmpty()),
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun savedPickerMaximumHeight(): Dp {
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    // Reserve the modal handle; the sheet owns IME/insets and content remains scrollable.
    return (height * 0.75f - 48.dp).coerceAtLeast(1.dp)
}

@Composable private fun SavedPickerSample(dark: Boolean, empty: Boolean = false,
    failure: SavedCollectionsFailure? = null, loading: Boolean = false, count: Int = 1) = CoffeePeekTheme(darkTheme = dark) {
    SavedPickerContent(SavedCollectionsState(SavedCollectionsMode.Picker, signedIn = true, loading = loading, failure = failure,
        lists = if (empty) emptyList() else List(count) { SavedCollectionSummary("list-$it", "Выходные с кофе ${it + 1}", itemCount = 4) },
        memberships = setOf("list-0"), membershipsLoaded = !loading && failure != SavedCollectionsFailure.Load), {}, 560.dp)
}
@Preview @Composable private fun SavedQuickCreateLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SavedQuickCreateContent(SavedCollectionsState(SavedCollectionsMode.Picker, signedIn = true, loading = false,
        quickTitle = "Выходные с кофе", membershipsLoaded = true), {}, 560.dp)
}
@Preview @Composable private fun SavedQuickCreateDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SavedQuickCreateContent(SavedCollectionsState(SavedCollectionsMode.Picker, signedIn = true, loading = false,
        quickTitle = "Выходные с кофе", membershipsLoaded = true), {}, 560.dp)
}
@Preview @Composable private fun SavedPickerLightPreview() = SavedPickerSample(false)
@Preview @Composable private fun SavedPickerDarkPreview() = SavedPickerSample(true)
@Preview @Composable private fun SavedPickerErrorLightPreview() = SavedPickerSample(false, true, SavedCollectionsFailure.Load)
@Preview @Composable private fun SavedPickerErrorDarkPreview() = SavedPickerSample(true, true, SavedCollectionsFailure.Load)
@Preview @Composable private fun SavedPickerEmptyLightPreview() = SavedPickerSample(false, true)
@Preview @Composable private fun SavedPickerEmptyDarkPreview() = SavedPickerSample(true, true)
@Preview @Composable private fun SavedPickerLoadingLightPreview() = SavedPickerSample(false, true, loading = true)
@Preview @Composable private fun SavedPickerLoadingDarkPreview() = SavedPickerSample(true, true, loading = true)
@Preview @Composable private fun SavedPickerLongListLightPreview() = SavedPickerSample(false, count = 30)
@Preview @Composable private fun SavedPickerLongListDarkPreview() = SavedPickerSample(true, count = 30)
