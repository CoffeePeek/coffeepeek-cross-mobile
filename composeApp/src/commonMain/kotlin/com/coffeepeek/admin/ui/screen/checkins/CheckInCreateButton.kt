package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_create_actions
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekAddButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInCreateButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val analyticsPage = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    CoffeePeekAddButton(onClick, stringResource(Res.string.check_in_create_actions),
        modifier.analyticsExposure("check_in", "global_create", analyticsPage))
}

@Preview @Composable private fun CheckInCreateButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreateButton(Modifier.padding(16.dp), onClick = {})
}

@Preview @Composable private fun CheckInCreateButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreateButton(Modifier.padding(16.dp), onClick = {})
}
