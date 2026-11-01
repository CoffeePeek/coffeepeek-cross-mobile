package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_overview_show_on_map
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopOverviewHeader(overview: ShopOverview, onOpenMap: () -> Unit,
    modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Text(overview.title, Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ShopTypeBadge(overview.type)
        }
        val canOpenMap = overview.latitude != null && overview.longitude != null
        val address = overview.address?.takeIf(String::isNotBlank)
            ?: if (canOpenMap) stringResource(Res.string.shop_overview_show_on_map) else null
        address?.let { label ->
            Row(Modifier.clickable(enabled = canOpenMap, role = Role.Button, onClick = onOpenMap),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                Icon(CoffeePeekIcons.Location, null, Modifier.size(14.dp))
                Text(label, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Preview @Composable private fun ShopOverviewHeaderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewHeader(previewOverview(), {})
}

@Preview @Composable private fun ShopOverviewHeaderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewHeader(previewOverview(), {})
}

private fun previewOverview() = ShopOverview("preview", "Кофейня", null,
    "Минск, улица Ленина, 1", 53.9, 27.5, 4.6, 12, true, emptyList())
