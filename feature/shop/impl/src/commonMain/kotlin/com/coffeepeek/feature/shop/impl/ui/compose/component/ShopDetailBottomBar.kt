package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_checkin
import com.coffeepeek.feature.shop.impl.resources.shop_detail_choose_another
import com.coffeepeek.feature.shop.impl.resources.shop_detail_continue
import com.coffeepeek.feature.shop.impl.resources.shop_detail_route
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDetailBottomBar(
    canOpenRoute: Boolean,
    forCheckIn: Boolean,
    onRoute: () -> Unit,
    onCheckIn: () -> Unit,
    onChooseAnotherShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing3),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        if (forCheckIn) {
            OutlinedButton(onClick = onChooseAnotherShop,
                modifier = Modifier.weight(1f).heightIn(min = CoffeePeekDimens.buttonHeight),
                shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius)) {
                Text(stringResource(Res.string.shop_detail_choose_another),
                    style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        } else {
            FilledIconButton(onClick = onRoute, enabled = canOpenRoute,
                modifier = Modifier.size(CoffeePeekDimens.buttonHeight)) {
                Icon(CoffeePeekIcons.Navigation, stringResource(Res.string.shop_detail_route))
            }
        }
        CoffeePeekButton(text = stringResource(if (forCheckIn) Res.string.shop_detail_continue
            else Res.string.shop_detail_checkin), onClick = onCheckIn, modifier = Modifier.weight(1f),
            leadingIcon = if (forCheckIn) null else ({ Icon(CoffeePeekIcons.Check, contentDescription = null) }))
    }
}

@Preview @Composable private fun ShopDetailBottomBarLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailBottomBar(true, false, {}, {}, {})
}

@Preview @Composable private fun ShopDetailBottomBarDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailBottomBar(false, false, {}, {}, {})
}

@Preview @Composable private fun ShopPickerBottomBarLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailBottomBar(true, true, {}, {}, {})
}

@Preview @Composable private fun ShopPickerBottomBarDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailBottomBar(true, true, {}, {}, {})
}
