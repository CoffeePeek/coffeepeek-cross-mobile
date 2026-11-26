package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_settings
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The owner's only shelf control, pinned above the app's floating navigation. */
@Composable
internal fun ShelfSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekIconButton(onClick, modifier.size(48.dp), contentDescription = stringResource(Res.string.shelf_settings)) {
        CoffeePeekIcon(CoffeePeekIcons.Settings, null, Modifier.size(24.dp))
    }
}

@Composable
private fun ShelfSettingsButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfSettingsButton({}, Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp))
}

@Preview @Composable private fun ShelfSettingsButtonLightPreview() = ShelfSettingsButtonPreview(false)
@Preview @Composable private fun ShelfSettingsButtonDarkPreview() = ShelfSettingsButtonPreview(true)
