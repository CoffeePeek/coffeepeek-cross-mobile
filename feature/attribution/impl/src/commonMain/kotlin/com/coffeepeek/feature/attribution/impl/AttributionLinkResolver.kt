package com.coffeepeek.feature.attribution.impl

import androidx.compose.runtime.*
import com.coffeepeek.core.designsystem.component.CoffeePeekActionDialog
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.attribution.api.AttributionFeature
import com.coffeepeek.feature.attribution.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The destination is applied only after a successful functional resolution. Never convert a failed link into a home view. */
@Composable
fun AttributionLinkResolver(code: String, feature: AttributionFeature?, onResolved: (String) -> Unit, onDismiss: () -> Unit) {
    var attempt by remember(code) { mutableIntStateOf(0) }
    var failed by remember(code) { mutableStateOf(false) }
    val resolved by rememberUpdatedState(onResolved)
    LaunchedEffect(code, attempt) {
        val path = feature?.open(code)?.getOrNull()
        if (path != null) resolved(path) else failed = true
    }
    if (failed) LinkFailure(onRetry = { failed = false; attempt++ }, onDismiss = onDismiss)
}

@Composable
private fun LinkFailure(onRetry: () -> Unit, onDismiss: () -> Unit) {
    CoffeePeekActionDialog(
        title = coffeePeekStringResource(Res.string.attribution_link_failed),
        message = coffeePeekStringResource(Res.string.attribution_link_failed_body),
        actions = listOf(CoffeePeekDialogAction(coffeePeekStringResource(Res.string.attribution_retry), onRetry)),
        cancelLabel = coffeePeekStringResource(Res.string.attribution_close),
        onDismiss = onDismiss,
    )
}

@Preview @Composable private fun LinkFailureLightPreview() = CoffeePeekTheme(darkTheme = false) { LinkFailure({}, {}) }
@Preview @Composable private fun LinkFailureDarkPreview() = CoffeePeekTheme(darkTheme = true) { LinkFailure({}, {}) }
