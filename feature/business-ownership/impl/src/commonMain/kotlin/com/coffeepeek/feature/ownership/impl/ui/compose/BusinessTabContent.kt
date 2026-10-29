package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BusinessTabContent(state: OwnershipState, action: (OwnershipAction) -> Unit,
    checkIns: () -> Unit, activity: @Composable () -> Unit = {}, edit: (BusinessKind, String) -> Unit = { _, _ -> }, open: (BusinessKind, String) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(16.dp).padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.ownership_business), style = MaterialTheme.typography.headlineLarge)
        OwnershipFeedback(state, action)
        if (state.loading) CoffeePeekLoader(stringResource(Res.string.ownership_loading))
        BusinessKind.entries.forEach { kind ->
            Text(stringResource(if (kind == BusinessKind.CoffeeShop) Res.string.ownership_shops else Res.string.ownership_roasters),
                style = MaterialTheme.typography.titleLarge)
            val businesses = if (kind == BusinessKind.CoffeeShop) state.shops else state.roasters?.items.orEmpty()
            businesses.forEach { business ->
                CoffeePeekContentCard {
                    CoffeePeekButton(business.name, { open(kind, business.id) })
                    CoffeePeekButton(stringResource(Res.string.ownership_edit), { edit(kind, business.id) })
                }
            }
        }
        state.roasters?.let { page -> OwnershipPager(page.page, page.hasNext) { action(OwnershipAction.RoastersPage(it)) } }
        activity()
        CoffeePeekButton(stringResource(Res.string.ownership_checkins), checkIns)
        CoffeePeekButton(stringResource(Res.string.ownership_refresh), { action(OwnershipAction.Refresh) })
    }
}
@Preview @Composable private fun BusinessTabLightPreview() = CoffeePeekTheme(darkTheme = false) {
    BusinessTabContent(OwnershipState(account = "preview"), {}, {}, open = { _, _ -> })
}
@Preview @Composable private fun BusinessTabDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    BusinessTabContent(OwnershipState(account = "preview"), {}, {}, open = { _, _ -> })
}
