package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_audience
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_audience_all
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_audience_only_me
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_public_hint
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_private_hint
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInVisibilityField(
    visibility: ShopCheckInVisibility, enabled: Boolean, onChange: (ShopCheckInVisibility) -> Unit,
) {
    val all = stringResource(Res.string.shop_checkin_audience_all)
    val onlyMe = stringResource(Res.string.shop_checkin_audience_only_me)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_checkin_audience), style = MaterialTheme.typography.titleSmall)
        CoffeePeekCapsuleSegmentedControl(
            options = listOf(ShopCheckInVisibility.Public, ShopCheckInVisibility.Private),
            selected = visibility, label = { if (it == ShopCheckInVisibility.Public) all else onlyMe },
            onSelected = onChange, modifier = Modifier.fillMaxWidth(), enabled = enabled)
        Text(stringResource(if (visibility == ShopCheckInVisibility.Public)
            Res.string.shop_checkin_form_public_hint else Res.string.shop_checkin_form_private_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun ShopCheckInVisibilityFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInVisibilityField(ShopCheckInVisibility.Private, true, {})
}
@Preview @Composable private fun ShopCheckInVisibilityFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInVisibilityField(ShopCheckInVisibility.Public, true, {})
}
