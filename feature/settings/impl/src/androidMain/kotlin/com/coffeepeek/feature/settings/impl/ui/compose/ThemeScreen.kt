package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgeColors
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.impl.R
import com.coffeepeek.feature.settings.impl.ui.ThemeViewModel
import com.coffeepeek.feature.settings.impl.ui.model.ThemeAction

@Composable
internal fun ThemeScreen(vm: ThemeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    ThemeContent(state.selected, { vm.onAction(ThemeAction.Select(it)) }, onBack, modifier)
}

@Composable
internal fun ThemeContent(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPage(stringResource(R.string.settings_theme_title), onBack, modifier) {
        SettingsHeading(stringResource(R.string.settings_theme_heading), stringResource(R.string.settings_theme_description))
        SettingsCard(Modifier.padding(horizontal = CoffeePeekDimens.spacing4)) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                ThemeOption(mode, mode == selected) { onSelect(mode) }
                if (index != ThemeMode.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = CoffeePeekDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoffeePeekIconBadge(mode.icon(), mode.iconColors())
        Spacer(Modifier.size(CoffeePeekDimens.spacing3))
        Text(stringResource(mode.title()), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(
            CoffeePeekIcons.Check, contentDescription = stringResource(R.string.settings_selected),
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(CoffeePeekDimens.settingsIconSize),
        )
    }
}

private fun ThemeMode.title() = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> CoffeePeekIcons.ThemeSystem
    ThemeMode.LIGHT -> CoffeePeekIcons.ThemeLight
    ThemeMode.DARK -> CoffeePeekIcons.ThemeDark
}

private fun ThemeMode.iconColors(): CoffeePeekIconBadgeColors = when (this) {
    ThemeMode.SYSTEM -> CoffeePeekIconBadgePalette.Cyan
    ThemeMode.LIGHT -> CoffeePeekIconBadgePalette.Gold
    ThemeMode.DARK -> CoffeePeekIconBadgePalette.Violet
}

@PreviewLightDark
@Composable
private fun ThemeContentPreview() = CoffeePeekTheme { Surface { ThemeContent(ThemeMode.DARK, {}, {}) } }
