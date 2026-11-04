package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTonalButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionSelectionToggle(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    CoffeePeekTonalButton({ onAction(CollectionsAction.SelectAll) }, Modifier.heightIn(min = 48.dp), enabled = !state.busy,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Text(stringResource(if (state.selected.isEmpty()) Res.string.collections_select_all else Res.string.collections_clear))
    }
}

@Preview @Composable private fun SelectionToggleLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CollectionSelectionToggle(CollectionsState(selected = setOf("place")), {})
}
@Preview @Composable private fun SelectionToggleDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CollectionSelectionToggle(CollectionsState(selected = setOf("place")), {})
}
