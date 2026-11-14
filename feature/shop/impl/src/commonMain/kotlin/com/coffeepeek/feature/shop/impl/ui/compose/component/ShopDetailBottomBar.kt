package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import com.coffeepeek.core.designsystem.component.coffeePeekViewportBottomPadding
import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledIconButton as FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekGlassButton
import com.coffeepeek.core.designsystem.component.CoffeePeekGlassButtonIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_checkin
import com.coffeepeek.feature.shop.impl.resources.shop_detail_route
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val ShopFooterMaxWidth = 353.dp

@Composable
internal fun ShopDetailBottomBar(
    canOpenRoute: Boolean,
    onRoute: () -> Unit,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    Box(
        modifier.fillMaxWidth().coffeePeekViewportBottomPadding().coffeePeekScreenBottomPadding()
            .padding(CoffeePeekDimens.spacing3),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = ShopFooterMaxWidth).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(onClick = onRoute, enabled = canOpenRoute,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CoffeePeekColors.Primary,
                    contentColor = CoffeePeekColors.LightTextOnPrimary),
                modifier = Modifier.then(if (canOpenRoute) Modifier.shopExposure(ShopExposure.Directions) else Modifier)
                    .size(CoffeePeekDimens.glassControlSize)) {
                CoffeePeekIcon(CoffeePeekIcons.Navigation, stringResource(Res.string.shop_detail_route), Modifier.size(20.dp))
            }
            ShopCheckInAction(onCheckIn, hazeState, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShopCheckInAction(onClick: () -> Unit, hazeState: HazeState?, modifier: Modifier) {
    CoffeePeekGlassButton(text = stringResource(Res.string.shop_detail_checkin), onClick = onClick,
        leadingIcon = CoffeePeekGlassButtonIcon.Check, hazeState = hazeState,
        modifier = modifier.heightIn(min = CoffeePeekDimens.glassControlSize).shopExposure(ShopExposure.CheckIn))
}

@Composable
private fun ShopFooterPreview(
    dark: Boolean,
    width: Dp = 417.dp,
    fontScale: Float = 1f,
    canOpenRoute: Boolean = true,
) = CoffeePeekTheme(darkTheme = dark) {
    val haze = rememberHazeState()
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        Box(Modifier.width(width).height(160.dp)) {
            Column(Modifier.fillMaxSize().hazeSource(haze)) {
                Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.primaryContainer))
                Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.secondaryContainer))
            }
            ShopDetailBottomBar(canOpenRoute, {}, {}, hazeState = haze,
                modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Preview @Composable private fun ShopDetailBottomBarLightPreview() = ShopFooterPreview(false)
@Preview @Composable private fun ShopDetailBottomBarDarkPreview() = ShopFooterPreview(true)

@Preview @Composable private fun ShopDetailBottomBarNarrowLightPreview() =
    ShopFooterPreview(false, width = 320.dp, fontScale = 2f, canOpenRoute = false)

@Preview @Composable private fun ShopDetailBottomBarNarrowDarkPreview() =
    ShopFooterPreview(true, width = 320.dp, fontScale = 2f, canOpenRoute = false)
