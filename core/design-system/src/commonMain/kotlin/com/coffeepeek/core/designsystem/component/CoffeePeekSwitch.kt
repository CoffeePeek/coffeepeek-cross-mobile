package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Theme-owned, full-size thumb in both states. The caller owns state and accessibility labels. */
@Composable
fun CoffeePeekSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (CoffeePeekNativeSwitch(checked, onCheckedChange, modifier, enabled)) return
    val palette = coffeePeekSwitchPalette(MaterialTheme.colorScheme.background.luminance() < 0.5f)
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = modifier,
        thumbContent = { Box(Modifier.size(SwitchDefaults.IconSize)) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = palette.checkedThumb, checkedTrackColor = palette.checkedTrack,
            uncheckedThumbColor = palette.uncheckedThumb, uncheckedTrackColor = palette.uncheckedTrack,
            checkedBorderColor = Color.Transparent, uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = palette.checkedThumb,
            disabledCheckedTrackColor = palette.checkedTrack.copy(alpha = 0.5f),
            disabledUncheckedThumbColor = palette.uncheckedThumb,
            disabledUncheckedTrackColor = palette.uncheckedTrack.copy(alpha = 0.5f),
            disabledCheckedBorderColor = Color.Transparent, disabledUncheckedBorderColor = Color.Transparent,
        ))
}

internal data class CoffeePeekSwitchPalette(
    val checkedThumb: Color,
    val checkedTrack: Color,
    val uncheckedThumb: Color,
    val uncheckedTrack: Color,
)

internal fun coffeePeekSwitchPalette(dark: Boolean) = if (dark) CoffeePeekSwitchPalette(
    CoffeePeekColors.SwitchCheckedThumbDark, CoffeePeekColors.SwitchCheckedTrackDark,
    CoffeePeekColors.SwitchCheckedThumbDark, CoffeePeekColors.DarkBorderHover,
) else CoffeePeekSwitchPalette(
    CoffeePeekColors.SwitchCheckedThumbLight, CoffeePeekColors.Primary,
    CoffeePeekColors.LightTextSecondary, CoffeePeekColors.LightBadge,
)

@Composable
private fun CoffeePeekSwitchPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        for (enabled in listOf(true, false)) for (checked in listOf(false, true)) {
            CoffeePeekSwitch(checked, {}, enabled = enabled)
        }
    }
}
@Preview @Composable private fun CoffeePeekSwitchLightPreview() = CoffeePeekSwitchPreview(false)
@Preview @Composable private fun CoffeePeekSwitchDarkPreview() = CoffeePeekSwitchPreview(true)
