package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekCreationSuccess
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_close
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_go_home
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_success_description
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_success_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AddShopSuccess(host: AddShopHost, onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { host.feedback(true) }
    AddShopSuccessContent(onGoHome, modifier)
}

@Composable
private fun AddShopSuccessContent(onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekCreationSuccess(
        title = stringResource(Res.string.contributions_shop_success_title),
        message = stringResource(Res.string.contributions_shop_success_description),
        buttonLabel = stringResource(Res.string.contributions_shop_go_home),
        closeLabel = stringResource(Res.string.contributions_close),
        onClose = onGoHome, onDone = onGoHome, modifier = modifier,
    )
}

@Composable
private fun AddShopSuccessPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AddShopSuccessContent({})
}

@Preview @Composable private fun AddShopSuccessLightPreview() = AddShopSuccessPreview(false)
@Preview @Composable private fun AddShopSuccessDarkPreview() = AddShopSuccessPreview(true)
