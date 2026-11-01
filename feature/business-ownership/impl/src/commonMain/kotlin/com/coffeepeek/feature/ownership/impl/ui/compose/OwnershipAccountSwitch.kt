package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.ownership.api.BusinessSessionState
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipAccountSwitch(state: BusinessSessionState, switch: () -> Unit) {
    if (!state.hasBusinesses) return
    Column {
        CoffeePeekSettingsSection(stringResource(Res.string.ownership_account)) {
            CoffeePeekSettingsRow(CoffeePeekIcons.CoffeeBean,
                stringResource(if (state.businessMode) Res.string.ownership_switch_personal else Res.string.ownership_switch_business),
                onClick = switch)
        }
        Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
    }
}
@Preview @Composable private fun OwnershipAccountSwitchLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OwnershipAccountSwitch(BusinessSessionState(hasBusinesses = true), {})
}
@Preview @Composable private fun OwnershipAccountSwitchDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OwnershipAccountSwitch(BusinessSessionState(true, true), {})
}
