package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_checkin
import com.coffeepeek.feature.shop.impl.resources.shop_detail_choose_another
import com.coffeepeek.feature.shop.impl.resources.shop_detail_continue
import com.coffeepeek.feature.shop.impl.resources.shop_detail_route
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import dev.chrisbanes.haze.HazeState

@Composable
internal fun ShopDetailBottomBar(
    canOpenRoute: Boolean,
    forCheckIn: Boolean,
    onRoute: () -> Unit,
    onCheckIn: () -> Unit,
    onChooseAnotherShop: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing3),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        if (forCheckIn) ShopPickerActions(onChooseAnotherShop, onCheckIn)
        else {
            CoffeePeekGlassIconButton(onClick = onRoute, enabled = canOpenRoute,
                contentDescription = stringResource(Res.string.shop_detail_route), hazeState = hazeState,
                modifier = Modifier.then(if (canOpenRoute) Modifier.shopExposure(ShopExposure.Directions) else Modifier)
                    .size(CoffeePeekDimens.buttonHeight)) {
                Icon(CoffeePeekIcons.Navigation, contentDescription = null, Modifier.size(20.dp))
            }
            ShopCheckInButton(onCheckIn, hazeState, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.ShopPickerActions(onChooseAnotherShop: () -> Unit, onCheckIn: () -> Unit) {
    OutlinedButton(onClick = onChooseAnotherShop,
        modifier = Modifier.weight(1f).heightIn(min = CoffeePeekDimens.buttonHeight),
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_detail_choose_another),
            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
    CoffeePeekButton(text = stringResource(Res.string.shop_detail_continue),
        onClick = onCheckIn, modifier = Modifier.weight(1f))
}

@Composable
private fun ShopCheckInButton(onClick: () -> Unit, hazeState: HazeState?, modifier: Modifier) {
    val shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius)
    OutlinedButton(onClick = onClick,
        modifier = modifier.shopExposure(ShopExposure.CheckIn).height(CoffeePeekDimens.buttonHeight)
            .coffeePeekLiquidGlass(shape, hazeState, shadowElevation = 8.dp),
        shape = shape, border = null,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface)) {
        Icon(CoffeePeekIcons.Check, null, Modifier.size(16.dp))
        Spacer(Modifier.width(CoffeePeekDimens.spacing1))
        Text(stringResource(Res.string.shop_detail_checkin), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold)
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
