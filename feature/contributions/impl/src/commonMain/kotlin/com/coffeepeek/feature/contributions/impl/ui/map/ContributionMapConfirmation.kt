package com.coffeepeek.feature.contributions.impl.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContributionMapConfirmation(state: ContributionMapUiState, compact: Boolean, onConfirm: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (compact) Row(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = CoffeePeekDimens.spacing5, vertical = CoffeePeekDimens.spacing2),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
                AddressSummary(state, compact, Modifier.weight(1f))
                ConfirmPointButton(state, compact, onConfirm, Modifier.width(144.dp))
            } else Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing5)) {
                AddressSummary(state, compact, Modifier.fillMaxWidth())
                Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                ConfirmPointButton(state, compact, onConfirm, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AddressSummary(state: ContributionMapUiState, compact: Boolean, modifier: Modifier) {
    Row(modifier.heightIn(min = CoffeePeekDimens.controlHeight).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (state.isResolving) CoffeePeekLoader(stringResource(Res.string.contributions_map_resolving), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        else Icon(CoffeePeekIcons.Location, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            if (!compact) {
                Text(stringResource(Res.string.contributions_map_address), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(CoffeePeekDimens.spacing1))
            }
            Text(addressText(state), style = MaterialTheme.typography.bodyMedium,
                color = if (state.hasCurrentAddress) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun addressText(state: ContributionMapUiState): String = when {
    state.isResolving -> stringResource(Res.string.contributions_map_resolving)
    state.hasCurrentAddress -> state.address.orEmpty()
    state.failed -> stringResource(Res.string.contributions_map_failed)
    else -> stringResource(Res.string.contributions_map_move)
}

@Composable
private fun ConfirmPointButton(state: ContributionMapUiState, compact: Boolean, onConfirm: () -> Unit, modifier: Modifier) {
    CoffeePeekButton(stringResource(if (compact) Res.string.contributions_map_choose_compact else Res.string.contributions_map_choose),
        onConfirm, modifier.testTag("shop_location_confirm"), enabled = !state.isResolving && state.hasCurrentAddress)
}

@Composable private fun ConfirmationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ContributionMapConfirmation(ContributionMapUiState(isResolving = true), false, {})
}
@Preview @Composable private fun ContributionMapConfirmationLightPreview() = ConfirmationPreview(false)
@Preview @Composable private fun ContributionMapConfirmationDarkPreview() = ConfirmationPreview(true)
