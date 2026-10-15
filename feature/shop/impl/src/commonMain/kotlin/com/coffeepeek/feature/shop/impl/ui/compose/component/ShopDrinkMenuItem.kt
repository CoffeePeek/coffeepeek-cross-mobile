package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDrinkMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick, modifier = Modifier.semantics { this.selected = selected },
        trailingIcon = {
            Box(Modifier.size(20.dp)) {
                if (selected) Icon(CoffeePeekIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        })
}

@Preview @Composable private fun ShopDrinkMenuItemLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { ShopDrinkMenuItem("Эспрессо", true, {}) }
}
@Preview @Composable private fun ShopDrinkMenuItemDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { ShopDrinkMenuItem("Эспрессо", false, {}) }
}
