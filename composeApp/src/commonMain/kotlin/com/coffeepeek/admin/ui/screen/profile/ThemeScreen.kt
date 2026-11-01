package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconColors
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ThemeScreen() {
    val selected by ThemeManager.themeMode.collectAsState()
    ThemeScreenContent(selected, ThemeManager::setTheme, Navigator::popBack)
}

@Composable
internal fun ThemeScreenContent(
    selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onBack: () -> Unit, copy: LegacyThemeCopy = legacyThemeCopy(),
) {
    CoffeePeekScaffold(topBar = { CpTopBar(copy.title, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = CpDimens.spacing4).verticalScroll(rememberScrollState())) {
            ThemeHeading(copy)
            CoffeePeekCard(shape = RoundedCornerShape(CpDimens.cardRadius),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = 0.dp,
                contentPadding = PaddingValues(0.dp)) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    ThemeOption(mode, mode == selected, copy) { onSelect(mode) }
                    if (index != ThemeMode.entries.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeHeading(copy: LegacyThemeCopy) {
    Spacer(Modifier.height(CpDimens.spacing4))
    Text(copy.heading, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(CpDimens.spacing2))
    Text(copy.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(CpDimens.spacing6))
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, copy: LegacyThemeCopy, onClick: () -> Unit) {
    val label = when (mode) {
        ThemeMode.SYSTEM -> copy.system
        ThemeMode.LIGHT -> copy.light
        ThemeMode.DARK -> copy.dark
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = CpDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically) {
        SettingsIconBadge(mode.icon(), mode.iconColors())
        Spacer(Modifier.size(CpDimens.spacing3))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(CpIcons.Check, copy.selected, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CpDimens.settingsIconSize))
    }
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> CpIcons.ThemeSystem
    ThemeMode.LIGHT -> CpIcons.ThemeLight
    ThemeMode.DARK -> CpIcons.ThemeDark
}

private fun ThemeMode.iconColors(): SettingsIconColors = when (this) {
    ThemeMode.SYSTEM -> SettingsIconPalette.Cyan
    ThemeMode.LIGHT -> SettingsIconPalette.Gold
    ThemeMode.DARK -> SettingsIconPalette.Violet
}

internal data class LegacyThemeCopy(
    val title: String, val heading: String, val description: String, val system: String,
    val light: String, val dark: String, val selected: String,
)

@Composable
private fun legacyThemeCopy() = LegacyThemeCopy(
    stringResource(Res.string.legacy_settings_theme), stringResource(Res.string.legacy_settings_theme_heading),
    stringResource(Res.string.legacy_settings_theme_page_description), stringResource(Res.string.legacy_settings_theme_system),
    stringResource(Res.string.legacy_settings_theme_light), stringResource(Res.string.legacy_settings_theme_dark),
    stringResource(Res.string.legacy_settings_selected),
)

@Composable
private fun LegacyThemePreview(dark: Boolean, belarusian: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val copy = if (belarusian) LegacyThemeCopy("Тэма", "Выгляд", "Абярыце, як CoffeePeek будзе выглядаць на вашай прыладзе.",
        "Сістэмная", "Светлая", "Цёмная", "Абрана")
    else LegacyThemeCopy("Тема", "Внешний вид", "Выберите, как CoffeePeek будет выглядеть на вашем устройстве.",
        "Системная", "Светлая", "Тёмная", "Выбрано")
    ThemeScreenContent(ThemeMode.DARK, {}, {}, copy)
}

@Preview @Composable private fun LegacyThemeRussianLightPreview() = LegacyThemePreview(false)
@Preview @Composable private fun LegacyThemeRussianDarkPreview() = LegacyThemePreview(true)
@Preview @Composable private fun LegacyThemeBelarusianLightPreview() = LegacyThemePreview(false, true)
@Preview @Composable private fun LegacyThemeBelarusianDarkPreview() = LegacyThemePreview(true, true)
