package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.ui.compose.component.SavedCollectionCard
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun SavedCollectionOwnerProfile(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    open: (String) -> Unit, modifier: Modifier = Modifier,
    image: @Composable (SavedCollectionSummary, Modifier) -> Unit = { _, m -> CoffeePeekImagePlaceholder(m) }) {
    when (val content = savedOwnerProfileContent(state)) {
        SavedOwnerProfileContent.Status -> SavedCollectionsStatus(state, action)
        SavedOwnerProfileContent.Empty -> Text(stringResource(Res.string.collections_library_empty), modifier.padding(horizontal = 16.dp))
        is SavedOwnerProfileContent.Lists -> LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(content.values, key = { it.id }) { value ->
                SavedCollectionCard(value, { open(value.id) }, Modifier.width(176.dp)) { image(value, it) }
            }
        }
    }
}

@Composable
private fun SavedOwnerProfileSample(dark: Boolean, failure: SavedCollectionsFailure? = null) = CoffeePeekTheme(darkTheme = dark) {
    val publicList = SavedCollectionSummary("public", "Кофейные выходные", visibility = SavedCollectionVisibility.Public, itemCount = 5)
    val privateList = SavedCollectionSummary("private", "Избранное", visibility = SavedCollectionVisibility.Private, itemCount = 2)
    SavedCollectionOwnerProfile(SavedCollectionsState(SavedCollectionsMode.Library, signedIn = true, loading = false,
        failure = failure, lists = listOf(publicList, privateList)), {}, {}, Modifier.width(320.dp))
}

@Preview @Composable private fun SavedOwnerProfileLightPreview() = SavedOwnerProfileSample(false)
@Preview @Composable private fun SavedOwnerProfileDarkPreview() = SavedOwnerProfileSample(true)
@Preview @Composable private fun SavedOwnerProfileFailureLightPreview() = SavedOwnerProfileSample(false, failure = SavedCollectionsFailure.Load)
@Preview @Composable private fun SavedOwnerProfileFailureDarkPreview() = SavedOwnerProfileSample(true, failure = SavedCollectionsFailure.Load)
