package com.coffeepeek.feature.shopreport.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReportCategoryOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(CpDimens.radiusLg),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(label, Modifier.padding(start = CpDimens.spacing1), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview @Composable private fun ShopReportCategoryOptionLightPreview() =
    CoffeePeekTheme(darkTheme = false) { ShopReportCategoryOption("Кофейня закрылась", true, {}) }

@Preview @Composable private fun ShopReportCategoryOptionDarkPreview() =
    CoffeePeekTheme(darkTheme = true) { ShopReportCategoryOption("Кофейня закрылась", false, {}) }
