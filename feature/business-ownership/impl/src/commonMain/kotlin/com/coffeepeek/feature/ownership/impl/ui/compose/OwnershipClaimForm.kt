package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipClaimForm(state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage = { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) },
    picker: OwnershipPhotoPicker = { _, _, content -> content {} }) {
    Text(stringResource(Res.string.ownership_claim_hint))
    ClaimInput(Res.string.ownership_contact, state.submission.contact, state.busy) { action(OwnershipAction.ClaimText(ClaimField.Contact, it)) }
    ClaimInput(Res.string.ownership_evidence, state.submission.evidence, state.busy) { action(OwnershipAction.ClaimText(ClaimField.Evidence, it)) }
    OwnershipEvidencePhotos(state, action, image, picker)
    Text(stringResource(Res.string.ownership_limits), style = MaterialTheme.typography.bodySmall)
    CoffeePeekButton(stringResource(Res.string.ownership_submit), { action(OwnershipAction.Submit) }, enabled = !state.busy)
}
@Composable
private fun ClaimInput(label: org.jetbrains.compose.resources.StringResource, value: String, busy: Boolean, change: (String) -> Unit) {
    val text = stringResource(label)
    CoffeePeekTextField(text, value, change, text, enabled = !busy)
}
@Preview @Composable private fun OwnershipClaimFormLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OwnershipClaimForm(OwnershipState(), {})
}
@Preview @Composable private fun OwnershipClaimFormDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OwnershipClaimForm(OwnershipState(), {})
}
