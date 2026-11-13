package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
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
    ownerShelf: OwnProfileShelf? = null, bottomPadding: Dp = 0.dp,
    header: @Composable () -> Unit = {},
) {
    var selectedName by rememberSaveable(stateKey) { mutableStateOf(ProfileContentTab.CheckIns.name) }
    val selected = ProfileContentTab.entries.firstOrNull { it.name == selectedName } ?: ProfileContentTab.CheckIns
    val requested = ownerShelf?.openRequested?.invoke() == true
    LaunchedEffect(requested) {
        if (requested) {
            selectedName = ProfileContentTab.Shelf.name
            ownerShelf?.onOpened?.invoke()
        }
    }
    val scroll = rememberScrollState()
    val select: (ProfileContentTab) -> Unit = { selectedName = it.name }
    // The editable shelf owns its scroll container. Move the header with its calendar state and effects.
    val tabHeader = remember(stateKey) {
        movableContentOf { tab: ProfileContentTab, onSelect: (ProfileContentTab) -> Unit, content: @Composable () -> Unit ->
            ProfileTabHeader(tab, onSelect, content)
        }
    }
    if (selected == ProfileContentTab.Shelf && ownerShelf != null) {
        ownerShelf.content(modifier) { tabHeader(selected, select, header) }
    } else Column(modifier.fillMaxWidth().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tabHeader(selected, select, header)
        when (selected) {
            ProfileContentTab.CheckIns -> checkIns()
            ProfileContentTab.Collections -> collections()
            ProfileContentTab.Shelf -> shelf()
        }
        if (bottomPadding > 0.dp) Spacer(Modifier.height(bottomPadding))
    }
}

@Composable
private fun ProfileTabHeader(
    selected: ProfileContentTab, onSelect: (ProfileContentTab) -> Unit, header: @Composable () -> Unit,
) {
    val labels = listOf(stringResource(Res.string.profile_section_check_ins),
        stringResource(Res.string.profile_section_favorites), stringResource(Res.string.profile_section_shelf))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        header()
        CoffeePeekUnderlineTabs(ProfileContentTab.entries, selected, { labels[it.ordinal] }, onSelect,
            Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun ProfileContentTabsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ProfileContentTabs("preview", { Text(stringResource(Res.string.profile_section_check_ins)) },
            { Text(stringResource(Res.string.profile_section_favorites)) },
            shelf = {}, ownerShelf = OwnProfileShelf(
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
