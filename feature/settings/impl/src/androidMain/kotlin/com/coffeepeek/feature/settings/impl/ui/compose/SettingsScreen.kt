package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsDivider
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsRow
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsSection
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.settings.api.SettingsAppInfo
import com.coffeepeek.feature.settings.api.SettingsExternalActions
import com.coffeepeek.feature.settings.api.SettingsSlots
import com.coffeepeek.feature.settings.domain.ThemeMode
import com.coffeepeek.feature.settings.impl.R
import com.coffeepeek.feature.settings.impl.ui.SettingsViewModel
import com.coffeepeek.feature.settings.impl.ui.model.SettingsAction
import com.coffeepeek.feature.settings.impl.ui.model.SettingsState

/** Where the tab can go; supplied by the caller. */
internal class SettingsNavigation(
    val onAddShop: () -> Unit, val onAddRoaster: () -> Unit, val onCity: () -> Unit,
    val onTheme: () -> Unit, val onUsagePolicy: () -> Unit, val onLogin: () -> Unit, val onRegister: () -> Unit,
)

@Composable
internal fun SettingsScreen(
    vm: SettingsViewModel, navigation: SettingsNavigation, external: SettingsExternalActions, slots: SettingsSlots,
    appInfo: SettingsAppInfo, bottomClearance: Dp, modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsContent(state, navigation, external, slots, appInfo, bottomClearance, { vm.onAction(SettingsAction.CheckForUpdates) }, modifier)
}

@Composable
internal fun SettingsContent(
    state: SettingsState, navigation: SettingsNavigation, external: SettingsExternalActions, slots: SettingsSlots,
    appInfo: SettingsAppInfo, bottomClearance: Dp, onCheckForUpdates: () -> Unit, modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding().verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.settings_tab_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.settingsPagePadding, vertical = CoffeePeekDimens.spacing4),
            )
            if (state.showGuestCard) {
                slots.guestCard(navigation.onLogin, navigation.onRegister, Modifier.padding(horizontal = CoffeePeekDimens.settingsPagePadding))
                Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            }
            if (state.showAddRows) {
                CoffeePeekSettingsSection(title = stringResource(R.string.settings_section_add)) {
                    CoffeePeekSettingsRow(
                        CoffeePeekIcons.Add, stringResource(R.string.settings_add_shop), stringResource(R.string.settings_add_shop_description),
                        navigation.onAddShop, iconColors = CoffeePeekIconBadgePalette.Gold,
                    )
                    CoffeePeekSettingsDivider()
                    CoffeePeekSettingsRow(
                        CoffeePeekIcons.CoffeeBean, stringResource(R.string.settings_add_roaster), stringResource(R.string.settings_add_roaster_description),
                        navigation.onAddRoaster, iconColors = CoffeePeekIconBadgePalette.Mint,
                    )
                }
                Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            }
            CoffeePeekSettingsSection(title = stringResource(R.string.settings_section_settings)) {
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Location, stringResource(R.string.settings_city_row), stringResource(R.string.settings_city_row_description),
                    navigation.onCity, iconColors = CoffeePeekIconBadgePalette.Aqua,
                    trailing = { RowValue(state.cityName ?: stringResource(R.string.settings_city_not_selected)) },
                )
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(
                    state.theme.icon(), stringResource(R.string.settings_theme_row), stringResource(R.string.settings_theme_row_description),
                    navigation.onTheme, iconColors = CoffeePeekIconBadgePalette.Violet,
                    trailing = { RowValue(stringResource(state.theme.valueLabel())) },
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            CoffeePeekSettingsSection(
                title = stringResource(R.string.settings_section_other),
                description = stringResource(R.string.settings_section_other_description),
            ) {
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Review, stringResource(R.string.settings_support), stringResource(R.string.settings_support_description),
                    external::openSupport, iconColors = CoffeePeekIconBadgePalette.Gold,
                )
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Lock, stringResource(R.string.settings_policy_row), null, navigation.onUsagePolicy,
                    iconColors = CoffeePeekIconBadgePalette.Emerald,
                )
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Share, stringResource(R.string.settings_share), null, external::shareApp,
                    iconColors = CoffeePeekIconBadgePalette.BrightCyan,
                )
                StoreReviewRow(external)
            }
            Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            CoffeePeekSettingsSection(title = stringResource(R.string.settings_section_update)) {
                val update = state.update
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Info,
                    stringResource(if (update.checking) R.string.settings_update_checking else R.string.settings_update_check),
                    update.message ?: if (update.updateAvailable) stringResource(R.string.settings_update_available)
                    else stringResource(R.string.settings_version, appInfo.versionName),
                    onCheckForUpdates,
                    enabled = !update.checking,
                    trailing = if (update.checking) {
                        { CoffeePeekLoader(stringResource(R.string.settings_loading), size = CoffeePeekDimens.settingsIconSize) }
                    } else null,
                )
            }
            Text(
                stringResource(R.string.settings_version, appInfo.versionName), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(
                    start = CoffeePeekDimens.settingsPagePadding, top = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.settingsPagePadding,
                ),
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing8 + bottomClearance))
        }
    }
}

@Composable
private fun RowValue(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(CoffeePeekDimens.settingsIconSize))
    }
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> CoffeePeekIcons.ThemeSystem
    ThemeMode.LIGHT -> CoffeePeekIcons.ThemeLight
    ThemeMode.DARK -> CoffeePeekIcons.ThemeDark
}

private fun ThemeMode.valueLabel() = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_value_system
    ThemeMode.LIGHT -> R.string.settings_theme_value_light
    ThemeMode.DARK -> R.string.settings_theme_value_dark
}

private val previewNavigation = SettingsNavigation({}, {}, {}, {}, {}, {}, {})
private val previewExternal = object : SettingsExternalActions {
    override val canRateApp = false
    override fun openSupport() = Unit
    override fun openTerms() = Unit
    override fun openPrivacy() = Unit
    override fun shareApp() = Unit
    override fun rateApp() = Unit
}

@Composable
private fun StoreReviewRow(external: SettingsExternalActions) {
    if (!external.canRateApp) return
    CoffeePeekSettingsDivider()
    CoffeePeekSettingsRow(
        CoffeePeekIcons.Star, stringResource(R.string.settings_rate_app),
        stringResource(R.string.settings_rate_app_description), external::rateApp,
        iconColors = CoffeePeekIconBadgePalette.Gold,
    )
}

@PreviewLightDark
@Composable
private fun SettingsStoreReviewPreview() = CoffeePeekTheme {
    CoffeePeekSettingsSection(title = "CoffeePeek") {
        StoreReviewRow(object : SettingsExternalActions by previewExternal { override val canRateApp = true })
    }
}
private val previewSlots = SettingsSlots(guestCard = { _, _, modifier -> Text("Войти", modifier) })

@PreviewLightDark
@Composable
private fun SettingsSignedInPreview() = CoffeePeekTheme {
    Surface {
        SettingsContent(SettingsState(true, ThemeMode.DARK, "Минск"), previewNavigation, previewExternal, previewSlots, SettingsAppInfo("1.0"), Dp.Hairline, {})
    }
}

@PreviewLightDark
@Composable
private fun SettingsGuestPreview() = CoffeePeekTheme {
    Surface {
        SettingsContent(SettingsState(false, ThemeMode.SYSTEM), previewNavigation, previewExternal, previewSlots, SettingsAppInfo("1.0"), Dp.Hairline, {})
    }
}
