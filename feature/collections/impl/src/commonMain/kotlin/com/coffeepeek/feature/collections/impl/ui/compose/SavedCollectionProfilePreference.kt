package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
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
internal fun SavedCollectionProfilePreference(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, modifier: Modifier = Modifier) {
    var showing by remember { mutableStateOf(false) }
    LaunchedEffect(state.signedIn, state.accountVersion) { showing = false }
    val choose = { if (state.signedIn) showing = true else action(SavedCollectionsAction.SignIn) }
    Column(modifier) {
        SavedProfileVisibilityRow(state) {
            if (state.featuredId == null) choose() else action(SavedCollectionsAction.Feature(null))
        }
        if (state.featuredId != null) {
            CoffeePeekSettingsDivider()
            SavedProfileSelectedRow(state, choose)
        }
        if (state.failure != null || state.loading) SavedCollectionsStatus(state, action)
    }
    if (showing) SavedProfileChoice(state, action) { showing = false }
}

@Composable
private fun SavedProfileVisibilityRow(state: SavedCollectionsState, onClick: () -> Unit) {
    val checked = state.featuredId != null
    val enabled = !state.loading && !state.busy && state.failure != SavedCollectionsFailure.Load
    CoffeePeekSettingsRow(CoffeePeekIcons.ListStar, stringResource(Res.string.collections_profile_show),
        stringResource(Res.string.collections_profile_choice_hint), onClick, enabled = enabled, role = Role.Switch,
        modifier = Modifier.semantics { toggleableState = ToggleableState(checked) }, showArrow = false,
        trailing = { CoffeePeekSwitch(checked, null, Modifier.clearAndSetSemantics { }, enabled) })
}

@Composable
private fun SavedProfileSelectedRow(state: SavedCollectionsState, onClick: () -> Unit) {
    CoffeePeekSettingsRow(CoffeePeekIcons.ListStar, stringResource(Res.string.collections_profile_list),
        state.lists.firstOrNull { it.id == state.featuredId && it.visibility == SavedCollectionVisibility.Public }?.title
            ?: stringResource(Res.string.collections_profile_choose), onClick,
        enabled = !state.loading && !state.busy && state.failure != SavedCollectionsFailure.Load)
}

@Composable
private fun SavedProfileChoice(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, dismiss: () -> Unit) {
    val maximumHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height.toDp() * 0.75f - 48.dp).coerceAtLeast(1.dp) }
    val choose: (String?) -> Unit = { id ->
        action(SavedCollectionsAction.Feature(id))
        dismiss()
    }
    CoffeePeekSwipeDismissModalBottomSheet(dismiss, stringResource(Res.string.collections_cancel)) {
        SavedProfileChoiceContent(state, choose, maximumHeight)
    }
}

@Composable
private fun SavedProfileChoiceContent(state: SavedCollectionsState, choose: (String?) -> Unit, maximumHeight: Dp) {
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = maximumHeight).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(Res.string.collections_profile_list), style = MaterialTheme.typography.titleLarge) }
        item { Text(stringResource(Res.string.collections_profile_choice_hint), style = MaterialTheme.typography.bodySmall) }
        item { CoffeePeekCheckmarkRow(stringResource(Res.string.collections_profile_none), state.featuredId == null, { choose(null) }) }
        val lists = state.lists.filter { it.visibility == SavedCollectionVisibility.Public }
        items(lists, key = { it.id }) { value -> CoffeePeekCheckmarkRow(value.title, value.id == state.featuredId, { choose(value.id) }) }
        if (lists.isEmpty()) item { Text(stringResource(Res.string.collections_profile_no_public), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable private fun SavedProfilePreferenceSample(dark: Boolean, selected: Boolean = true) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionProfilePreference(SavedCollectionsState(SavedCollectionsMode.Preference, signedIn = true, loading = false,
        lists = listOf(SavedCollectionSummary("sample", "Кофейные выходные", visibility = SavedCollectionVisibility.Public)),
        featuredId = "sample".takeIf { selected }), {})
}
@Preview @Composable private fun SavedPreferenceLightPreview() = SavedProfilePreferenceSample(false)
@Preview @Composable private fun SavedPreferenceDarkPreview() = SavedProfilePreferenceSample(true)
@Preview @Composable private fun SavedPreferenceHiddenLightPreview() = SavedProfilePreferenceSample(false, false)
@Preview @Composable private fun SavedPreferenceHiddenDarkPreview() = SavedProfilePreferenceSample(true, false)
@Preview @Composable private fun SavedPreferenceChoiceLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SavedProfileChoiceContent(SavedCollectionsState(SavedCollectionsMode.Preference, signedIn = true, loading = false), {}, 400.dp)
}
@Preview @Composable private fun SavedPreferenceChoiceDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SavedProfileChoiceContent(SavedCollectionsState(SavedCollectionsMode.Preference, signedIn = true, loading = false), {}, 400.dp)
}
