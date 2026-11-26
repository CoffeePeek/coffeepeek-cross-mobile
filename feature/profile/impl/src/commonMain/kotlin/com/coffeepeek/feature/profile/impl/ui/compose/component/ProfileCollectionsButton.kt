package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_section_all_description
import com.coffeepeek.feature.profile.impl.resources.profile_section_favorites
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The profile has one entry to the owner's full collection library. */
@Composable
internal fun ProfileCollectionsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekIconButton(onClick, modifier.size(48.dp),
        contentDescription = stringResource(Res.string.profile_section_all_description,
            stringResource(Res.string.profile_section_favorites))) {
        CoffeePeekIcon(CoffeePeekIcons.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun ProfileCollectionsButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Box(Modifier.padding(16.dp)) { ProfileCollectionsButton({}) } }
}
@Preview @Composable private fun ProfileCollectionsButtonLightPreview() = ProfileCollectionsButtonPreview(false)
@Preview @Composable private fun ProfileCollectionsButtonDarkPreview() = ProfileCollectionsButtonPreview(true)
