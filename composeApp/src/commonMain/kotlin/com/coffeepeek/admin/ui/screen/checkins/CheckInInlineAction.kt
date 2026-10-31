package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_add
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Labeled action in the Home list; flow, auth and analytics remain app-owned. */
@Composable
internal fun CheckInInlineAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val page = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    CheckInInlineActionContent(onClick, modifier.analyticsExposure("check_in", "global_create", page))
}

@Composable
private fun CheckInInlineActionContent(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekButton(stringResource(Res.string.check_in_add), onClick, modifier)
}

@Preview @Composable private fun CheckInInlineActionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInInlineActionContent({})
}

@Preview @Composable private fun CheckInInlineActionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInInlineActionContent({})
}
