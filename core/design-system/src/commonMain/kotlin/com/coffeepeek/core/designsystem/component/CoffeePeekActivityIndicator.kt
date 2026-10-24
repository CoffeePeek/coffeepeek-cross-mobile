package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

/** An inline "working" spinner: the platform's own when it installs one, CoffeePeek's loader otherwise. */
@Composable
fun CoffeePeekActivityIndicator(
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = CoffeePeekDimens.loaderButton,
) {
    val native = LocalCoffeePeekNativeControlsPresenter.current
    if (native != null && native.ActivityIndicator(contentDescription, modifier)) return
    CoffeePeekLoader(contentDescription, modifier, size = size)
}
