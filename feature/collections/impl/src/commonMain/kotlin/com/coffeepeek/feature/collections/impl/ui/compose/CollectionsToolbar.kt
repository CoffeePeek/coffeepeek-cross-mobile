package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionsToolbar(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    val title = when (state.page) {
        CollectionsPage.Selection -> stringResource(Res.string.collections_title)
        CollectionsPage.Links -> stringResource(Res.string.collections_links)
        CollectionsPage.Preview -> state.collection?.title ?: stringResource(Res.string.collections_default_title)
    }
    CoffeePeekTopBar(title, stringResource(Res.string.collections_back), onBack = { onAction(CollectionsAction.Back) }, actions = {
        if (state.signedIn && !state.publicView && state.page == CollectionsPage.Selection) {
            CoffeePeekPlainIconButton({ onAction(CollectionsAction.OpenLinks) }, enabled = !state.busy && !state.loading) {
                Icon(CoffeePeekIcons.ListStar, stringResource(Res.string.collections_links))
            }
        }
    })
}

@Preview @Composable private fun CollectionsToolbarLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CollectionsToolbar(CollectionsState(signedIn = true, loading = false), {})
}
@Preview @Composable private fun CollectionsToolbarDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CollectionsToolbar(CollectionsState(signedIn = true, loading = false), {})
}
