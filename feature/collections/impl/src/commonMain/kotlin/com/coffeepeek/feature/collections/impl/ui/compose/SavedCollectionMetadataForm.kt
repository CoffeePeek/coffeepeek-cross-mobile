package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionMetadataForm(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, pick: () -> Unit,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            SavedCollectionCoverEditor(state, action, pick, image)
            CoffeePeekTextField(stringResource(Res.string.collections_name), state.title,
                { action(SavedCollectionsAction.Title(it)) }, stringResource(Res.string.collections_name),
                enabled = !state.busy, modifier = Modifier.weight(1f))
        }
        SavedCollectionDescription(state, action)
        CoffeePeekGroupSection { SavedCollectionVisibilityRow(state, action) }
    }
}

@Composable
private fun SavedCollectionVisibilityRow(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    val isPublic = state.visibility == SavedCollectionVisibility.Public
    val hint = stringResource(if (isPublic) Res.string.collections_public_hint else Res.string.collections_private_hint)
    Row(Modifier.fillMaxWidth().toggleable(isPublic, enabled = !state.busy, role = Role.Switch,
        onValueChange = { action(SavedCollectionsAction.Visibility(if (it) SavedCollectionVisibility.Public else SavedCollectionVisibility.Private)) })
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.collections_public_switch), style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CoffeePeekSwitch(isPublic, null, Modifier.clearAndSetSemantics { }, enabled = !state.busy)
    }
}

@Composable
private fun SavedCollectionCoverEditor(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    pick: () -> Unit, image: @Composable (Modifier) -> Unit) {
    Column(Modifier.width(112.dp)) {
        Box(Modifier.size(112.dp)) {
            image(Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)))
            CoffeePeekIconButton(pick, Modifier.align(Alignment.BottomEnd).offset(8.dp, 8.dp),
                enabled = !state.busy && !state.preparingCover,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface),
                contentDescription = stringResource(Res.string.collections_change_cover), hazeState = null) {
                CoffeePeekIcon(CoffeePeekIcons.Edit, null, Modifier.size(20.dp))
            }
        }
        if (state.coverInput != null || state.collection?.summary?.cover != null && !state.removeCover)
            CoffeePeekTextButton({ action(SavedCollectionsAction.RemoveCover) }, enabled = !state.busy) {
                Text(stringResource(Res.string.collections_remove_cover), style = MaterialTheme.typography.labelSmall)
            }
    }
}

@Composable
private fun SavedCollectionDescription(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    val description = stringResource(Res.string.collections_description)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(description, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekCompactOutlinedTextField(state.description, { action(SavedCollectionsAction.Description(it)) },
            Modifier.fillMaxWidth().semantics { contentDescription = description }, enabled = !state.busy,
            singleLine = false, minLines = 3, maxLines = 6,
            placeholder = { Text(stringResource(Res.string.collections_description_placeholder)) },
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = CoffeePeekDimens.inputPadding, vertical = 12.dp))
    }
}

@Composable private fun SavedMetadataSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionMetadataForm(SavedCollectionsState(SavedCollectionsMode.Editor, signedIn = true, loading = false,
        title = "Любимые места", description = "Для хорошего начала дня"), {}, {})
}
@Preview @Composable private fun SavedMetadataLightPreview() = SavedMetadataSample(false)
@Preview @Composable private fun SavedMetadataDarkPreview() = SavedMetadataSample(true)
