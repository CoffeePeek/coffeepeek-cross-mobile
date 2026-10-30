package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.domain.model.OwnershipProblem
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipFeedback(state: OwnershipState, action: (OwnershipAction) -> Unit) {
    if (state.saved) Text(stringResource(Res.string.ownership_saved), color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    state.problem?.let { issue ->
        CoffeePeekContentCard {
            Text(stringResource(problemText(issue)), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            val retry = when {
                state.reloadRequired && state.profile != null -> OwnershipAction.AskReload
                state.destination == OwnershipDestination.Submit -> OwnershipAction.Submit
                state.destination == OwnershipDestination.Editor && state.profile != null -> OwnershipAction.AskSave
                else -> OwnershipAction.Refresh
            }
            CoffeePeekButton(stringResource(if (retry == OwnershipAction.AskReload) Res.string.ownership_reload else Res.string.ownership_retry),
                { action(retry) }, enabled = !state.busy && !state.loading)
        }
    }
    if (state.busy) CoffeePeekLoader(contentDescription = stringResource(Res.string.ownership_loading))
}
private fun problemText(issue: OwnershipProblem) = when (issue) {
    OwnershipProblem.Unavailable -> Res.string.ownership_error_unavailable
    OwnershipProblem.SignedOut -> Res.string.ownership_error_signedout
    OwnershipProblem.NotFound -> Res.string.ownership_error_notfound
    OwnershipProblem.Conflict -> Res.string.ownership_error_conflict
    OwnershipProblem.Validation -> Res.string.ownership_error_validation
    OwnershipProblem.PartialSave -> Res.string.ownership_error_partial
}
@Preview @Composable private fun OwnershipFeedbackLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OwnershipFeedback(OwnershipState(problem = OwnershipProblem.Conflict), {})
}
@Preview @Composable private fun OwnershipFeedbackDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OwnershipFeedback(OwnershipState(problem = OwnershipProblem.Conflict), {})
}
