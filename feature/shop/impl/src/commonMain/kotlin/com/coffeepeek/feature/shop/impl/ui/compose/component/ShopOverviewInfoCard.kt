package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopOverviewInfoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
) {
    Surface(modifier, shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.semantics(mergeDescendants = true) {}
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier)
            .heightIn(min = 48.dp).padding(CoffeePeekDimens.spacing2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // Product decision: all three information-card icons keep the brand accent.
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showChevron) Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview @Composable private fun ShopOverviewInfoCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewInfoCard("4.6", "Чекинов: 12", CoffeePeekIcons.StarFilled, onClick = {}, showChevron = true)
}

@Preview @Composable private fun ShopOverviewInfoCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewInfoCard("4.6", "Чекинов: 12", CoffeePeekIcons.StarFilled, onClick = {}, showChevron = true)
}
