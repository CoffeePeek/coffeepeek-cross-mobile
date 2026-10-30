package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_contact_copy_phone
import com.coffeepeek.feature.shop.impl.resources.shop_contact_open
import com.coffeepeek.feature.shop.impl.ui.data.ContactLink
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopContactRow(link: ContactLink, icon: ImageVector, onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier, onCopy: (() -> Unit)? = null) {
    val openLabel = stringResource(Res.string.shop_contact_open, link.label)
    Row(modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = openLabel) { onOpenLink(link.target) }
        .heightIn(min = 56.dp).padding(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing1,
            top = CoffeePeekDimens.spacing1, bottom = CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        Icon(icon, null, Modifier.size(24.dp))
        Text(link.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onCopy != null) IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
            Icon(CoffeePeekIcons.Copy, stringResource(Res.string.shop_contact_copy_phone), Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview @Composable private fun ShopContactRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopContactRow(ContactLink("+375 29 123 45 67", "tel:+375291234567"), CoffeePeekIcons.Phone, {}, onCopy = {})
}
@Preview @Composable private fun ShopContactRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopContactRow(ContactLink("+375 29 123 45 67", "tel:+375291234567"), CoffeePeekIcons.Phone, {}, onCopy = {})
}
