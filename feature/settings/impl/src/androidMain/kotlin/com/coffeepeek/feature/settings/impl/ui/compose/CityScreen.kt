package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.impl.R
import com.coffeepeek.feature.settings.impl.ui.CityViewModel
import com.coffeepeek.feature.settings.impl.ui.model.CityAction
import com.coffeepeek.feature.settings.impl.ui.model.CityLoad
import com.coffeepeek.feature.settings.impl.ui.model.CityState

@Composable
internal fun CityScreen(vm: CityViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    CityContent(state, { vm.onAction(CityAction.Select(it)) }, { vm.onAction(CityAction.Load) }, onBack, modifier)
}

@Composable
internal fun CityContent(state: CityState, onSelect: (String) -> Unit, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPage(stringResource(R.string.settings_city_title), onBack, modifier) {
        SettingsHeading(stringResource(R.string.settings_city_heading), stringResource(R.string.settings_city_description))
        when {
            state.load == CityLoad.Loading -> Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(R.string.settings_loading))
            }
            state.load == CityLoad.Failed -> CityMessage(R.string.settings_city_error, onRetry)
            state.cities.isEmpty() -> CityMessage(R.string.settings_city_empty, onRetry)
            else -> SettingsCard(Modifier.padding(horizontal = CoffeePeekDimens.spacing4)) {
                state.cities.forEachIndexed { index, city ->
                    CityOption(city, city.id == state.selectedId) { onSelect(city.id) }
                    if (index != state.cities.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun CityMessage(text: Int, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.settings_retry)) }
    }
}

@Composable
private fun CityOption(city: SettingsCity, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = CoffeePeekDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoffeePeekIconBadge(CoffeePeekIcons.Location, CoffeePeekIconBadgePalette.Aqua)
        Spacer(Modifier.size(CoffeePeekDimens.spacing3))
        Text(city.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(
            CoffeePeekIcons.Check, contentDescription = stringResource(R.string.settings_selected),
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(CoffeePeekDimens.settingsIconSize),
        )
    }
}

private val previewCities = listOf(SettingsCity("minsk", "Минск"), SettingsCity("brest", "Брест"))

@PreviewLightDark
@Composable
private fun CityContentPreview() = CoffeePeekTheme {
    Surface { CityContent(CityState(CityLoad.Loaded, previewCities, "brest"), {}, {}, {}) }
}

@PreviewLightDark
@Composable
private fun CityErrorPreview() = CoffeePeekTheme { Surface { CityContent(CityState(CityLoad.Failed), {}, {}, {}) } }
