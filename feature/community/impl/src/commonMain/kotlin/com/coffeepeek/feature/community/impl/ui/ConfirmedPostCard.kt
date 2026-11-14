package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ConfirmedPostCard(onOpen: () -> Unit, onRefresh: () -> Unit) {
    CoffeePeekCard {
        Text(stringResource(Res.string.community_saved_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.community_saved_body), style = MaterialTheme.typography.bodyMedium)
        Row {
            CoffeePeekTextButton(onOpen) { Text(stringResource(Res.string.community_saved_open)) }
            CoffeePeekTextButton(onRefresh) { Text(stringResource(Res.string.community_refresh)) }
        }
    }
}

@Composable private fun ConfirmedPostPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ConfirmedPostCard({}, {})
}
@Preview @Composable private fun ConfirmedPostLightPreview() = ConfirmedPostPreview(false)
@Preview @Composable private fun ConfirmedPostDarkPreview() = ConfirmedPostPreview(true)
