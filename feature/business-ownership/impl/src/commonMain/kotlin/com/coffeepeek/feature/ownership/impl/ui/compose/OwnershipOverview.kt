package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipOverview(state: OwnershipState, action: (OwnershipAction) -> Unit) {
    if (state.claims == null) return
    Text(stringResource(Res.string.ownership_claims), style = MaterialTheme.typography.titleLarge)
    if (state.claims.items.isEmpty()) Text(stringResource(Res.string.ownership_empty))
    state.claims.items.forEach { claim -> OwnershipClaimRow(claim) { action(OwnershipAction.OpenClaim(claim.id)) } }
    OwnershipPager(state.claims.page, state.claims.hasNext) { action(OwnershipAction.ClaimsPage(it)) }
    OwnedBusinessList(stringResource(Res.string.ownership_shops), state.shops, action)
    if (!state.roasters?.items.isNullOrEmpty()) {
        OwnedBusinessList(stringResource(Res.string.ownership_roasters), state.roasters?.items.orEmpty(), action)
        state.roasters?.let { page -> OwnershipPager(page.page, page.hasNext) { action(OwnershipAction.RoastersPage(it)) } }
    }
}
@Composable
private fun OwnedBusinessList(title: String, businesses: List<OwnedBusiness>, action: (OwnershipAction) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    if (businesses.isEmpty()) Text(stringResource(Res.string.ownership_empty))
    businesses.forEach { business -> CoffeePeekButton(business.name, { action(OwnershipAction.OpenBusiness(business.kind, business.id)) }) }
}
@Composable
private fun OwnershipClaimRow(claim: OwnershipClaim, open: () -> Unit) {
    CoffeePeekContentCard {
        Text(claim.name, fontWeight = FontWeight.SemiBold)
        Text(claimStatus(claim.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(if (claim.hasCurrentAccess) Res.string.ownership_access_yes else Res.string.ownership_access_no),
            style = MaterialTheme.typography.bodySmall)
        CoffeePeekButton(stringResource(Res.string.ownership_claim_title), open)
    }
}
@Composable
internal fun OwnershipPager(page: Int, hasNext: Boolean, change: (Int) -> Unit) {
    if (page == 1 && !hasNext) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekButton(stringResource(Res.string.ownership_previous), { change(page - 1) }, enabled = page > 1)
        CoffeePeekButton(stringResource(Res.string.ownership_next), { change(page + 1) }, enabled = hasNext)
    }
}
@Composable
internal fun claimStatus(status: ClaimStatus): String = stringResource(when (status) {
    ClaimStatus.Pending -> Res.string.ownership_pending
    ClaimStatus.Approved -> Res.string.ownership_approved
    ClaimStatus.Rejected -> Res.string.ownership_rejected
})
@Preview @Composable private fun OwnershipOverviewLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OwnershipOverview(OwnershipState(claims = BusinessPage(emptyList(), 0, 1, 20)), {})
}
@Preview @Composable private fun OwnershipOverviewDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OwnershipOverview(OwnershipState(claims = BusinessPage(emptyList(), 0, 1, 20)), {})
}
