package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledIconButton as FilledIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_add
import coffeepeek.composeapp.generated.resources.continue_action
import coffeepeek.composeapp.generated.resources.shop_detail_choose_another
import coffeepeek.composeapp.generated.resources.shop_detail_loading
import coffeepeek.composeapp.generated.resources.shop_detail_route
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekAddButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ShopDetailBottomBar(
    isCheckInLoading: Boolean,
    canOpenRoute: Boolean,
    onRoute: () -> Unit,
    onCheckIn: () -> Unit,
    onChooseAnotherShop: () -> Unit,
    modifier: Modifier = Modifier,
    forCheckIn: Boolean = false,
    hazeState: HazeState? = null,
) {
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(CpDimens.spacing3).height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        if (forCheckIn) ShopPickerActions(isCheckInLoading, onChooseAnotherShop, onCheckIn)
        else {
            FilledIconButton(onRoute, enabled = canOpenRoute, shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CoffeePeekColors.Primary,
                    contentColor = CoffeePeekColors.LightTextOnPrimary),
                modifier = Modifier.then(if (canOpenRoute)
                    Modifier.analyticsExposure("directions", "shop_detail", "shop_detail") else Modifier)
                    .size(CpDimens.buttonHeight)) {
                Icon(CpIcons.Navigation, stringResource(Res.string.shop_detail_route), Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            ShopCheckInButton(isCheckInLoading, onCheckIn,
                Modifier.analyticsExposure("check_in", "shop_detail", "shop_detail"))
        }
    }
}

@Composable
private fun RowScope.ShopPickerActions(loading: Boolean, onChooseAnotherShop: () -> Unit, onCheckIn: () -> Unit) {
    OutlinedButton(onChooseAnotherShop, enabled = !loading,
        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = CpDimens.buttonHeight),
        shape = RoundedCornerShape(CpDimens.buttonRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = CpDimens.spacing2)) {
        Text(stringResource(Res.string.shop_detail_choose_another),
            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
    AppButton(text = stringResource(if (loading) Res.string.shop_detail_loading else Res.string.continue_action),
        onClick = onCheckIn, enabled = !loading, modifier = Modifier.weight(1f).fillMaxHeight())
}

@Composable
private fun ShopCheckInButton(loading: Boolean, onClick: () -> Unit, modifier: Modifier) {
    CoffeePeekAddButton(contentDescription = stringResource(Res.string.check_in_add), onClick = onClick,
        isLoading = loading, loadingDescription = stringResource(Res.string.shop_detail_loading),
        modifier = modifier)
}

@Composable
private fun ShopFooterPreview(dark: Boolean, forCheckIn: Boolean = false, loading: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val hazeState = rememberHazeState()
    Box(Modifier.fillMaxWidth().height(160.dp)) {
        Column(Modifier.fillMaxSize().hazeSource(hazeState)) {
            Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.primaryContainer))
            Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.secondaryContainer))
        }
        ShopDetailBottomBar(isCheckInLoading = loading, canOpenRoute = !loading,
            onRoute = {}, onCheckIn = {}, onChooseAnotherShop = {},
            modifier = Modifier.align(Alignment.BottomCenter), forCheckIn = forCheckIn, hazeState = hazeState)
    }
}

@Preview @Composable private fun ShopFooterLightPreview() = ShopFooterPreview(false)
@Preview @Composable private fun ShopFooterDarkPreview() = ShopFooterPreview(true)
@Preview @Composable private fun ShopPickerFooterLightPreview() = ShopFooterPreview(false, forCheckIn = true)
@Preview @Composable private fun ShopPickerFooterDarkPreview() = ShopFooterPreview(true, forCheckIn = true)
@Preview @Composable private fun ShopLoadingFooterLightPreview() = ShopFooterPreview(false, loading = true)
@Preview @Composable private fun ShopLoadingFooterDarkPreview() = ShopFooterPreview(true, loading = true)
