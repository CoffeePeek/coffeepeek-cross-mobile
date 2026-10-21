package com.coffeepeek.feature.shopreport.impl.ui.compose.component

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReportCategoryOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
        // A check is visual feedback for a single selection, not an independent checkbox.
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) Icon(CoffeePeekIcons.Check, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview @Composable private fun ShopReportCategoryOptionLightPreview() =
    CoffeePeekTheme(darkTheme = false) { Surface { ShopReportCategoryOption("Кофейня закрылась", true, {}) } }

@Preview @Composable private fun ShopReportCategoryOptionDarkPreview() =
    CoffeePeekTheme(darkTheme = true) { Surface { ShopReportCategoryOption("Кофейня закрылась", false, {}) } }
