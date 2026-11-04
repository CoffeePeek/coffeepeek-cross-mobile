package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionsStatus(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.loading) SavedCollectionsLoadingStatus()
        if (!state.signedIn && state.mode !in setOf(SavedCollectionsMode.Public, SavedCollectionsMode.Featured)) {
            Text(stringResource(Res.string.collections_sign_in_hint))
            CoffeePeekButton(stringResource(Res.string.collections_sign_in), { action(SavedCollectionsAction.SignIn) })
        }
        state.failure?.let { failure ->
            Text(stringResource(when (failure) {
                SavedCollectionsFailure.Load -> Res.string.collections_saved_load_failed
                SavedCollectionsFailure.Save -> Res.string.collections_saved_save_failed
                SavedCollectionsFailure.Conflict -> Res.string.collections_conflict
                SavedCollectionsFailure.Cover -> Res.string.collections_cover_failed
                SavedCollectionsFailure.Membership -> Res.string.collections_membership_failed
                SavedCollectionsFailure.Delete -> Res.string.collections_delete_failed
            }), color = MaterialTheme.colorScheme.error)
            if (failure in setOf(SavedCollectionsFailure.Load, SavedCollectionsFailure.Conflict)) {
                CoffeePeekTextButton({ action(SavedCollectionsAction.Reload) }, enabled = !state.busy && !state.loading) {
                    Text(stringResource(Res.string.collections_retry))
                }
            }
        }
    }
}

@Composable
private fun SavedCollectionsLoadingStatus() {
    val loading = stringResource(Res.string.collections_loading)
    CoffeePeekShimmer(Modifier.fillMaxWidth().height(48.dp).semantics {
        contentDescription = loading
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    })
}

@Preview @Composable private fun SavedCollectionsStatusLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SavedCollectionsStatus(SavedCollectionsState(SavedCollectionsMode.Library, loading = false, failure = SavedCollectionsFailure.Load), {})
}
@Preview @Composable private fun SavedCollectionsStatusDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SavedCollectionsStatus(SavedCollectionsState(SavedCollectionsMode.Library, loading = false, failure = SavedCollectionsFailure.Load), {})
}
@Preview @Composable private fun SavedCollectionsLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SavedCollectionsLoadingStatus()
}
@Preview @Composable private fun SavedCollectionsLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SavedCollectionsLoadingStatus()
}
