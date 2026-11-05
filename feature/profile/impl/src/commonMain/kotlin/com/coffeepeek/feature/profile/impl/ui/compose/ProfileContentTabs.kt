package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekUnderlineTabs
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_section_check_ins
import com.coffeepeek.feature.profile.impl.resources.profile_section_favorites
import com.coffeepeek.feature.profile.impl.resources.profile_section_shelf
import org.jetbrains.compose.ui.tooling.preview.Preview

private enum class ProfileContentTab { CheckIns, Collections, Shelf }

/** Only the selected section is composed; callers retain ownership of each feature's data and routes. */
@Composable
internal fun ProfileContentTabs(
    stateKey: String, checkIns: @Composable () -> Unit, collections: @Composable () -> Unit,
    shelf: @Composable () -> Unit, modifier: Modifier = Modifier,
) {
    var selectedName by rememberSaveable(stateKey) { mutableStateOf(ProfileContentTab.CheckIns.name) }
    val selected = ProfileContentTab.entries.firstOrNull { it.name == selectedName } ?: ProfileContentTab.CheckIns
    val labels = listOf(stringResource(Res.string.profile_section_check_ins),
        stringResource(Res.string.profile_section_favorites), stringResource(Res.string.profile_section_shelf))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekUnderlineTabs(ProfileContentTab.entries, selected, { labels[it.ordinal] }, { selectedName = it.name },
            Modifier.padding(horizontal = 16.dp))
        when (selected) {
            ProfileContentTab.CheckIns -> checkIns()
            ProfileContentTab.Collections -> collections()
            ProfileContentTab.Shelf -> shelf()
        }
    }
}

@Composable
private fun ProfileContentTabsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ProfileContentTabs("preview", { Text(stringResource(Res.string.profile_section_check_ins)) },
            { Text(stringResource(Res.string.profile_section_favorites)) },
            { Text(stringResource(Res.string.profile_section_shelf)) })
    }
}

@Preview @Composable private fun ProfileContentTabsLightPreview() = ProfileContentTabsPreview(false)
@Preview @Composable private fun ProfileContentTabsDarkPreview() = ProfileContentTabsPreview(true)
