package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.impl.ui.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun SavedCollectionOwnerProfile(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    open: (String) -> Unit, fallback: @Composable () -> Unit, modifier: Modifier = Modifier,
    image: @Composable (SavedCollectionSummary, Modifier) -> Unit = { _, m -> CoffeePeekImagePlaceholder(m) }) {
    when (val content = savedOwnerProfileContent(state)) {
        SavedOwnerProfileContent.Favorites -> Box(modifier) { fallback() }
        is SavedOwnerProfileContent.Selected -> SavedCollectionFeatured(content.value, open, modifier.padding(horizontal = 16.dp)) { image(content.value, it) }
        SavedOwnerProfileContent.Status -> Box(modifier) {
            val status = if (state.signedIn && !state.loading && state.failure == null)
                state.copy(failure = SavedCollectionsFailure.Load) else state
            SavedCollectionsStatus(status, action)
        }
    }
}

@Composable
private fun SavedOwnerProfileSample(dark: Boolean, selected: Boolean = true, failure: SavedCollectionsFailure? = null) = CoffeePeekTheme(darkTheme = dark) {
    val value = SavedCollectionSummary("sample", "Кофейные выходные", visibility = SavedCollectionVisibility.Public, itemCount = 5)
    SavedCollectionOwnerProfile(SavedCollectionsState(SavedCollectionsMode.Preference, signedIn = true, loading = false,
        failure = failure, lists = listOf(value), featuredId = value.id.takeIf { selected }), {}, {},
        { Text("Избранное") }, Modifier.width(320.dp))
}

@Preview @Composable private fun SavedOwnerProfileLightPreview() = SavedOwnerProfileSample(false)
@Preview @Composable private fun SavedOwnerProfileDarkPreview() = SavedOwnerProfileSample(true)
@Preview @Composable private fun SavedOwnerProfileFallbackLightPreview() = SavedOwnerProfileSample(false, false)
@Preview @Composable private fun SavedOwnerProfileFallbackDarkPreview() = SavedOwnerProfileSample(true, false)
@Preview @Composable private fun SavedOwnerProfileFailureLightPreview() = SavedOwnerProfileSample(false, failure = SavedCollectionsFailure.Load)
@Preview @Composable private fun SavedOwnerProfileFailureDarkPreview() = SavedOwnerProfileSample(true, failure = SavedCollectionsFailure.Load)
