package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Alignment
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileCollectionsButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.profile.api.OwnProfileHistory
import com.coffeepeek.feature.profile.api.OwnProfileShelf
import com.coffeepeek.core.designsystem.component.CoffeePeekUnderlineTabs
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_section_check_ins
import com.coffeepeek.feature.profile.impl.resources.profile_section_favorites
import com.coffeepeek.feature.profile.impl.resources.profile_section_shelf
import com.coffeepeek.feature.profile.impl.resources.profile_title
import org.jetbrains.compose.ui.tooling.preview.Preview

private enum class ProfileContentTab { CheckIns, Collections, Shelf }

/** Only the selected section is composed; callers retain ownership of each feature's data and routes. */
@Composable
internal fun ProfileContentTabs(
    stateKey: String, checkIns: @Composable () -> Unit, collections: @Composable () -> Unit,
    shelf: @Composable () -> Unit, modifier: Modifier = Modifier,
    ownerShelf: OwnProfileShelf? = null, ownerHistory: OwnProfileHistory? = null, bottomPadding: Dp = 0.dp,
    checkInsList: (@Composable (Modifier, @Composable () -> Unit) -> Unit)? = null,
    onOpenCollections: (() -> Unit)? = null,
    header: @Composable (onSelectCheckIns: () -> Unit) -> Unit = {},
) {
    var selectedName by rememberSaveable(stateKey) { mutableStateOf(ProfileContentTab.CheckIns.name) }
    val selected = ProfileContentTab.entries.firstOrNull { it.name == selectedName } ?: ProfileContentTab.CheckIns
    LaunchedEffect(ownerHistory?.selectionRevision) {
        if ((ownerHistory?.selectionRevision ?: 0) > 0) selectedName = ProfileContentTab.CheckIns.name
    }
    val requested = ownerShelf?.openRequested?.invoke() == true
    LaunchedEffect(requested) {
        if (requested) {
            selectedName = ProfileContentTab.Shelf.name
            ownerShelf?.onOpened?.invoke()
        }
    }
    val scrolls = remember(stateKey) { ProfileContentTab.entries.associateWith { ScrollState(0) } }
    val scroll = scrolls.getValue(selected)
    val select: (ProfileContentTab) -> Unit = { selectedName = it.name }
    // The editable shelf owns its scroll container. Move the header with its calendar state and effects.
    val tabHeader = remember(stateKey) {
        movableContentOf { tab: ProfileContentTab, onSelect: (ProfileContentTab) -> Unit,
            content: @Composable (() -> Unit) -> Unit ->
            ProfileTabHeader(tab, onSelect, content)
        }
    }
    ProfileTabPane(selected, scroll, modifier, bottomPadding, ownerShelf, ownerHistory, checkInsList,
        onOpenCollections, { tabHeader(selected, select, header) }, checkIns, collections, shelf)
}

@Composable
private fun ProfileTabPane(
    selected: ProfileContentTab, scroll: ScrollState, modifier: Modifier, bottomPadding: Dp,
    ownerShelf: OwnProfileShelf?, ownerHistory: OwnProfileHistory?,
    checkInsList: (@Composable (Modifier, @Composable () -> Unit) -> Unit)?, onOpenCollections: (() -> Unit)?,
    header: @Composable () -> Unit, checkIns: @Composable () -> Unit,
    collections: @Composable () -> Unit, shelf: @Composable () -> Unit,
) {
    val contentBottomPadding = bottomPadding + if (selected == ProfileContentTab.Collections && onOpenCollections != null) 64.dp else 0.dp
    Box(modifier.fillMaxSize()) {
        if (selected == ProfileContentTab.CheckIns && ownerHistory != null) {
            ownerHistory.list(header)
        } else if (selected == ProfileContentTab.Shelf && ownerShelf != null) {
            ownerShelf.content(Modifier.matchParentSize(), header)
        } else if (selected == ProfileContentTab.CheckIns && checkInsList != null) {
            checkInsList(Modifier.matchParentSize(), header)
        } else Column(Modifier.fillMaxWidth().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            header()
            when (selected) {
                ProfileContentTab.CheckIns -> checkIns()
                ProfileContentTab.Collections -> collections()
                ProfileContentTab.Shelf -> shelf()
            }
            if (contentBottomPadding > 0.dp) Spacer(Modifier.height(contentBottomPadding))
        }
        if (selected == ProfileContentTab.Collections && onOpenCollections != null) {
            ProfileCollectionsButton(onOpenCollections, Modifier.align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomPadding))
        }
    }
}

@Composable
private fun ProfileTabHeader(
    selected: ProfileContentTab, onSelect: (ProfileContentTab) -> Unit, header: @Composable (() -> Unit) -> Unit,
) {
    val labels = listOf(stringResource(Res.string.profile_section_check_ins),
        stringResource(Res.string.profile_section_favorites), stringResource(Res.string.profile_section_shelf))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        header { if (selected != ProfileContentTab.CheckIns) onSelect(ProfileContentTab.CheckIns) }
        CoffeePeekUnderlineTabs(ProfileContentTab.entries, selected, { labels[it.ordinal] }, onSelect,
            Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun ProfileContentTabsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ProfileContentTabs("preview", { Text(stringResource(Res.string.profile_section_check_ins)) },
            { ProfileCollectionsPreviewCards() },
            shelf = {}, modifier = Modifier.size(320.dp, 600.dp), ownerShelf = OwnProfileShelf(
                content = { modifier, header ->
                    Column(modifier.verticalScroll(rememberScrollState())) {
                        header()
                        Text(stringResource(Res.string.profile_section_shelf))
                    }
                },
                openRequested = { false }, onOpened = {},
            )) {
            Text(stringResource(Res.string.profile_title), Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Preview @Composable private fun ProfileContentTabsLightPreview() = ProfileContentTabsPreview(false)
@Preview @Composable private fun ProfileContentTabsDarkPreview() = ProfileContentTabsPreview(true)

@Composable
private fun ProfileCollectionsPanePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ProfileTabPane(ProfileContentTab.Collections, rememberScrollState(), Modifier.size(320.dp, 600.dp),
            bottomPadding = 100.dp, ownerShelf = null, ownerHistory = null, checkInsList = null, onOpenCollections = {},
            header = { ProfileTabHeader(ProfileContentTab.Collections, {}, {}) }, checkIns = {}, shelf = {},
            collections = { ProfileCollectionsPreviewCards() })
    }
}

@Composable
private fun ProfileCollectionsPreviewCards() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(2) {
                    Surface(Modifier.weight(1f).aspectRatio(1f), shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant) {}
                }
            }
        }
    }
}

@Preview @Composable private fun ProfileCollectionsPaneLightPreview() = ProfileCollectionsPanePreview(false)
@Preview @Composable private fun ProfileCollectionsPaneDarkPreview() = ProfileCollectionsPanePreview(true)
