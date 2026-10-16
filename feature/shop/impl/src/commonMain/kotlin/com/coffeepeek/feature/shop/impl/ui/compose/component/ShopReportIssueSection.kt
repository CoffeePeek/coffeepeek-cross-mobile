package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_report_hint
import com.coffeepeek.feature.shop.impl.resources.shop_report_prompt
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReportIssueSection(onReport: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.clickable(role = Role.Button, onClick = onReport)
            .heightIn(min = 56.dp).padding(CoffeePeekDimens.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Icon(CoffeePeekIcons.Info, null, Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                Text(stringResource(Res.string.shop_report_prompt), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.shop_report_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}

@Preview @Composable private fun ShopReportIssueSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReportIssueSection({})
}

@Preview @Composable private fun ShopReportIssueSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReportIssueSection({})
}
