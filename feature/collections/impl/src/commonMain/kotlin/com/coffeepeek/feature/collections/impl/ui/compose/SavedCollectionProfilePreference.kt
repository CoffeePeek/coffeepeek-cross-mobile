package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    LaunchedEffect(state.signedIn) { if (!state.signedIn) showing = false }
    Column(modifier) {
        CoffeePeekSettingsRow(CoffeePeekIcons.ListStar, stringResource(Res.string.collections_profile_list),
            state.lists.firstOrNull { it.id == state.featuredId }?.title ?: stringResource(Res.string.collections_profile_none),
            onClick = { if (state.signedIn) showing = true else action(SavedCollectionsAction.SignIn) },
            enabled = !state.loading && !state.busy && state.failure != SavedCollectionsFailure.Load)
        if (state.failure != null) SavedCollectionsStatus(state, action)
    }
    if (showing) SavedProfileChoice(state, action) { showing = false }
}

@Composable
private fun SavedProfileChoice(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, dismiss: () -> Unit) {
    val choose: (String?) -> Unit = { id ->
        action(SavedCollectionsAction.Feature(id))
        dismiss()
    }
    CoffeePeekSwipeDismissModalBottomSheet(dismiss, stringResource(Res.string.collections_cancel), maxSheetHeightFraction = 0.8f) {
        LazyColumn(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(stringResource(Res.string.collections_profile_list), style = MaterialTheme.typography.titleLarge) }
            item { Text(stringResource(Res.string.collections_profile_choice_hint), style = MaterialTheme.typography.bodySmall) }
            item { CoffeePeekCheckmarkRow(stringResource(Res.string.collections_profile_none), state.featuredId == null,
                { choose(null) }) }
            items(state.lists.filter { it.visibility == SavedCollectionVisibility.Public }, key = { it.id }) { value ->
                CoffeePeekCheckmarkRow(value.title, value.id == state.featuredId, { choose(value.id) })
            }
        }
    }
}

@Composable private fun SavedProfilePreferenceSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionProfilePreference(SavedCollectionsState(SavedCollectionsMode.Preference, signedIn = true, loading = false,
        lists = listOf(SavedCollectionSummary("sample", "Кофейные выходные", visibility = SavedCollectionVisibility.Public)), featuredId = "sample"), {})
}
@Preview @Composable private fun SavedPreferenceLightPreview() = SavedProfilePreferenceSample(false)
@Preview @Composable private fun SavedPreferenceDarkPreview() = SavedProfilePreferenceSample(true)
