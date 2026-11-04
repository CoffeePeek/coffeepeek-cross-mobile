package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.feature.ownership.api.OwnershipImage
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipClaimDetails(claim: OwnershipClaim, image: OwnershipImage = { _, _, m, _ -> CoffeePeekImagePlaceholder(m) }) {
    Text(claim.name, style = MaterialTheme.typography.headlineSmall)
    Text(claim.slug)
    Text(claimStatus(claim.status), color = MaterialTheme.colorScheme.primary)
    Text(stringResource(if (claim.hasCurrentAccess) Res.string.ownership_access_yes else Res.string.ownership_access_no))
    CoffeePeekContentCard {
        Text(stringResource(Res.string.ownership_contact), fontWeight = FontWeight.SemiBold)
        Text(claim.contact)
        Text(stringResource(Res.string.ownership_evidence), fontWeight = FontWeight.SemiBold)
        Text(claim.evidence)
        Text(stringResource(Res.string.ownership_claim_created), fontWeight = FontWeight.SemiBold)
        Text(claim.createdAt)
    }
    claim.evidencePhotos.forEach { photo -> image(null, photo.bytes, Modifier.fillMaxWidth().height(160.dp), ContentScale.Fit) }
    if (claim.status != ClaimStatus.Pending) CoffeePeekContentCard {
        Text(stringResource(Res.string.ownership_decision), fontWeight = FontWeight.SemiBold)
        Text(claim.decisionReason ?: stringResource(Res.string.ownership_no_reason))
        claim.reviewedAt?.let { Text(it) }
    }
}
private fun previewClaim() = OwnershipClaim("claim", BusinessKind.Roaster, "business", "Пример обжарщика", "example",
    "owner@example.test", "Представитель бизнеса", ClaimStatus.Approved, "2026-10-28", null, "2026-10-28", false)
@Preview @Composable private fun OwnershipClaimDetailsLightPreview() = CoffeePeekTheme(darkTheme = false) { OwnershipClaimDetails(previewClaim()) }
@Preview @Composable private fun OwnershipClaimDetailsDarkPreview() = CoffeePeekTheme(darkTheme = true) { OwnershipClaimDetails(previewClaim()) }
