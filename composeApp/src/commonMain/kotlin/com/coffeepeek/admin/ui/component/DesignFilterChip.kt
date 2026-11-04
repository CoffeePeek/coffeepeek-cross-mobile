package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DesignFilterChip(
    label: String, selected: Boolean, onClick: () -> Unit,
    leadingIcon: ImageVector? = null, trailingIcon: ImageVector? = null,
) = CoffeePeekFilterChip(label, selected, onClick, leadingIcon = leadingIcon, trailingIcon = trailingIcon)

@Preview @Composable private fun DesignFilterChipLightPreview() = CoffeePeekTheme(darkTheme = false) {
    DesignFilterChip("Открыто", false, {}, CoffeePeekIcons.Time)
}

@Preview @Composable private fun DesignFilterChipDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    DesignFilterChip("Открыто", false, {}, CoffeePeekIcons.Time)
}
