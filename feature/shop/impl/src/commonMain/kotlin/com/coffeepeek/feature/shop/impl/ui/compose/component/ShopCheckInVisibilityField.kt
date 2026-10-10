package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_public
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_public_hint
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_private_hint
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInVisibilityField(
    visibility: ShopCheckInVisibility, enabled: Boolean, onChange: (ShopCheckInVisibility) -> Unit,
) {
    val public = visibility == ShopCheckInVisibility.Public
    Row(
        Modifier.fillMaxWidth().toggleable(
            value = public, enabled = enabled, role = Role.Switch,
            onValueChange = { onChange(if (it) ShopCheckInVisibility.Public else ShopCheckInVisibility.Private) },
        ), verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.shop_checkin_form_public), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (public) Res.string.shop_checkin_form_public_hint else Res.string.shop_checkin_form_private_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = public, onCheckedChange = null, enabled = enabled)
    }
}

@Preview @Composable private fun ShopCheckInVisibilityFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInVisibilityField(ShopCheckInVisibility.Private, true, {})
}
@Preview @Composable private fun ShopCheckInVisibilityFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInVisibilityField(ShopCheckInVisibility.Public, true, {})
}
