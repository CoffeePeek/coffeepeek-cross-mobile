package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.CollectionRules
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionsBottomActions(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.page == CollectionsPage.Selection) CollectionCreateAction(state, onAction)
            else CollectionExportActions(state, onAction)
        }
    }
}

@Composable
private fun CollectionCreateAction(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    val title = stringResource(Res.string.collections_default_title)
    val focus = LocalFocusManager.current
    Text(stringResource(Res.string.collections_selected, state.selected.size),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    CoffeePeekButton(stringResource(if (state.busy) Res.string.collections_preparing else Res.string.collections_create),
        onClick = { focus.clearFocus(); onAction(CollectionsAction.Create(title)) },
        enabled = !state.busy && state.selected.isNotEmpty() && CollectionRules.validTitle(state.title.ifBlank { title }))
}

@Composable
private fun CollectionExportActions(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    CoffeePeekButton(stringResource(if (state.busy) Res.string.collections_preparing else Res.string.collections_share_image),
        { onAction(CollectionsAction.ShareImages) }, enabled = !state.busy && state.collection?.places?.isNotEmpty() == true)
    if (state.collection?.url?.isNotBlank() == true) CoffeePeekTextButton({ onAction(CollectionsAction.ShareLink) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
        Text(stringResource(Res.string.collections_share_link))
    }
}

@Preview @Composable private fun CollectionsBottomActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CollectionsBottomActions(CollectionsState(signedIn = true, loading = false), {})
}
@Preview @Composable private fun CollectionsBottomActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CollectionsBottomActions(CollectionsState(signedIn = true, loading = false), {})
}
