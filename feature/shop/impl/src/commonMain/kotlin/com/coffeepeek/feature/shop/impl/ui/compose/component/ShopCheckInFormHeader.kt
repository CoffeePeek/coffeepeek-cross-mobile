package com.coffeepeek.feature.shop.impl.ui.compose.component

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_title
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInFormHeader(shopName: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(stringResource(Res.string.shop_checkin_form_title), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (shopName.isNotBlank()) Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(CoffeePeekIcons.Location, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(shopName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Preview @Composable private fun ShopCheckInFormHeaderLightPreview() = CoffeePeekTheme(darkTheme = false) { ShopCheckInFormHeader("Memories") }
@Preview @Composable private fun ShopCheckInFormHeaderDarkPreview() = CoffeePeekTheme(darkTheme = true) { ShopCheckInFormHeader("Memories") }
