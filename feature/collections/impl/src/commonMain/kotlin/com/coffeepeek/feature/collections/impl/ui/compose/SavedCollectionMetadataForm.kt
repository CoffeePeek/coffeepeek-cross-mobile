package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionMetadataForm(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, pick: () -> Unit,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CoffeePeekTextField(stringResource(Res.string.collections_name), state.title, { action(SavedCollectionsAction.Title(it)) },
            stringResource(Res.string.collections_name), enabled = !state.busy)
        val description = stringResource(Res.string.collections_description)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(description, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CoffeePeekCompactOutlinedTextField(state.description, { action(SavedCollectionsAction.Description(it)) },
                Modifier.fillMaxWidth().semantics { contentDescription = description }, enabled = !state.busy,
                singleLine = false, minLines = 3, maxLines = 6,
                placeholder = { Text(stringResource(Res.string.collections_description_placeholder)) }, shape = RoundedCornerShape(16.dp))
        }
        CoffeePeekGroupSection(footer = stringResource(if (state.visibility == SavedCollectionVisibility.Public)
            Res.string.collections_public_hint else Res.string.collections_private_hint)) {
            CoffeePeekSwitchRow(stringResource(Res.string.collections_public_switch), state.visibility == SavedCollectionVisibility.Public,
                { action(SavedCollectionsAction.Visibility(if (it) SavedCollectionVisibility.Public else SavedCollectionVisibility.Private)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            image(Modifier.size(112.dp).clip(RoundedCornerShape(16.dp)))
            Column {
                CoffeePeekTextButton(pick, enabled = !state.busy) { Text(stringResource(Res.string.collections_change_cover)) }
                if (state.coverInput != null || state.collection?.summary?.cover != null && !state.removeCover)
                    CoffeePeekTextButton({ action(SavedCollectionsAction.RemoveCover) }, enabled = !state.busy) {
                        Text(stringResource(Res.string.collections_remove_cover))
                    }
            }
        }
    }
}

@Composable private fun SavedMetadataSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionMetadataForm(SavedCollectionsState(SavedCollectionsMode.Editor, signedIn = true, loading = false,
        title = "Любимые места", description = "Для хорошего начала дня"), {}, {})
}
@Preview @Composable private fun SavedMetadataLightPreview() = SavedMetadataSample(false)
@Preview @Composable private fun SavedMetadataDarkPreview() = SavedMetadataSample(true)
