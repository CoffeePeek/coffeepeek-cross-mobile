package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_roaster_catalog
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FavoriteRoasterStat(icon: ImageVector, label: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (count > 0) count.toString() else "—", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Preview @Composable private fun FavoriteRoasterStatLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoriteRoasterStat(CoffeePeekIcons.CoffeeBean, stringResource(Res.string.favorites_roaster_catalog), 12)
}
@Preview @Composable private fun FavoriteRoasterStatDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoriteRoasterStat(CoffeePeekIcons.CoffeeBean, stringResource(Res.string.favorites_roaster_catalog), 0)
}
