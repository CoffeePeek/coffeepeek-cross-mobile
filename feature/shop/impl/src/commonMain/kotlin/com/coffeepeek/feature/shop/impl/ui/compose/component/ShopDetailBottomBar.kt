package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledIconButton as FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekAddButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_create
import com.coffeepeek.feature.shop.impl.resources.shop_detail_choose_another
import com.coffeepeek.feature.shop.impl.resources.shop_detail_continue
import com.coffeepeek.feature.shop.impl.resources.shop_detail_route
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
            FilledIconButton(onClick = onRoute, enabled = canOpenRoute,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CoffeePeekColors.Primary,
                    contentColor = CoffeePeekColors.LightTextOnPrimary),
                modifier = Modifier.then(if (canOpenRoute) Modifier.shopExposure(ShopExposure.Directions) else Modifier)
                    .size(CoffeePeekDimens.buttonHeight)) {
                Icon(CoffeePeekIcons.Navigation, stringResource(Res.string.shop_detail_route), Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            ShopCheckInAddButton(onCheckIn)
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
private fun ShopCheckInAddButton(onClick: () -> Unit) {
    CoffeePeekAddButton(contentDescription = stringResource(Res.string.shop_checkins_create), onClick = onClick,
        modifier = Modifier.shopExposure(ShopExposure.CheckIn))
}

@Composable
private fun ShopFooterPreview(dark: Boolean, forCheckIn: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val haze = rememberHazeState()
    Box(Modifier.fillMaxWidth().height(160.dp)) {
        Column(Modifier.fillMaxSize().hazeSource(haze)) {
            Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.primaryContainer))
            Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.secondaryContainer))
        }
        ShopDetailBottomBar(true, forCheckIn, {}, {}, {}, hazeState = haze,
            modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview @Composable private fun ShopDetailBottomBarLightPreview() = ShopFooterPreview(false)
@Preview @Composable private fun ShopDetailBottomBarDarkPreview() = ShopFooterPreview(true)
@Preview @Composable private fun ShopPickerBottomBarLightPreview() = ShopFooterPreview(false, true)
@Preview @Composable private fun ShopPickerBottomBarDarkPreview() = ShopFooterPreview(true, true)
