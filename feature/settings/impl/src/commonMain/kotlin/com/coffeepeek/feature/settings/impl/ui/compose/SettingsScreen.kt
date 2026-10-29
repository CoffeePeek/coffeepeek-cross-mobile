package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.settings.impl.resources.Res
import com.coffeepeek.feature.settings.impl.resources.settings_add_roaster
import com.coffeepeek.feature.settings.impl.resources.settings_add_roaster_description
import com.coffeepeek.feature.settings.impl.resources.settings_add_shop
import com.coffeepeek.feature.settings.impl.resources.settings_add_shop_description
import com.coffeepeek.feature.settings.impl.resources.settings_city_not_selected
import com.coffeepeek.feature.settings.impl.resources.settings_city_row
import com.coffeepeek.feature.settings.impl.resources.settings_city_row_description
import com.coffeepeek.feature.settings.impl.resources.settings_loading
import com.coffeepeek.feature.settings.impl.resources.settings_policy_row
import com.coffeepeek.feature.settings.impl.resources.settings_rate_app
import com.coffeepeek.feature.settings.impl.resources.settings_rate_app_description
import com.coffeepeek.feature.settings.impl.resources.settings_section_add
import com.coffeepeek.feature.settings.impl.resources.settings_section_other
import com.coffeepeek.feature.settings.impl.resources.settings_section_other_description
import com.coffeepeek.feature.settings.impl.resources.settings_section_settings
import com.coffeepeek.feature.settings.impl.resources.settings_section_update
import com.coffeepeek.feature.settings.impl.resources.settings_share
import com.coffeepeek.feature.settings.impl.resources.settings_support
import com.coffeepeek.feature.settings.impl.resources.settings_support_description
import com.coffeepeek.feature.settings.impl.resources.settings_tab_title
import com.coffeepeek.feature.settings.impl.resources.settings_theme_row
import com.coffeepeek.feature.settings.impl.resources.settings_theme_row_description
import com.coffeepeek.feature.settings.impl.resources.settings_theme_value_dark
import com.coffeepeek.feature.settings.impl.resources.settings_theme_value_light
import com.coffeepeek.feature.settings.impl.resources.settings_theme_value_system
import com.coffeepeek.feature.settings.impl.resources.settings_update_available
import com.coffeepeek.feature.settings.impl.resources.settings_update_check
import com.coffeepeek.feature.settings.impl.resources.settings_update_checking
import com.coffeepeek.feature.settings.impl.resources.settings_update_failed
import com.coffeepeek.feature.settings.impl.resources.settings_update_up_to_date
import com.coffeepeek.feature.settings.impl.resources.settings_version
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityIndicator
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
import com.coffeepeek.feature.settings.impl.ui.SettingsViewModel
import com.coffeepeek.feature.settings.impl.ui.model.SettingsAction
import com.coffeepeek.feature.settings.impl.ui.model.SettingsEvent
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val upToDate = stringResource(Res.string.settings_update_up_to_date)
    val failed = stringResource(Res.string.settings_update_failed)
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { event ->
                val message = when (event) {
                    SettingsEvent.UpToDate -> upToDate
                    SettingsEvent.UpdateCheckFailed -> failed
                }
                slots.notify(message)
            }
        }
    }
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
                stringResource(Res.string.settings_tab_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.settingsPagePadding, vertical = CoffeePeekDimens.spacing4),
            )
            if (state.showGuestCard) {
                slots.guestCard(navigation.onLogin, navigation.onRegister, Modifier.padding(horizontal = CoffeePeekDimens.settingsPagePadding))
                Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            }
            slots.accountSwitch()
            if (state.showAddRows) {
                CoffeePeekSettingsSection(title = stringResource(Res.string.settings_section_add)) {
                    CoffeePeekSettingsRow(
                        CoffeePeekIcons.Add, stringResource(Res.string.settings_add_shop), stringResource(Res.string.settings_add_shop_description),
                        navigation.onAddShop, iconColors = CoffeePeekIconBadgePalette.Gold,
                    )
                    CoffeePeekSettingsDivider()
                    CoffeePeekSettingsRow(
                        CoffeePeekIcons.CoffeeBean, stringResource(Res.string.settings_add_roaster), stringResource(Res.string.settings_add_roaster_description),
                        navigation.onAddRoaster, iconColors = CoffeePeekIconBadgePalette.Mint,
                    )
                }
                Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            }
            CoffeePeekSettingsSection(title = stringResource(Res.string.settings_section_settings)) {
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Location, stringResource(Res.string.settings_city_row), stringResource(Res.string.settings_city_row_description),
                    navigation.onCity, iconColors = CoffeePeekIconBadgePalette.Aqua,
                    trailing = { RowValue(state.cityName ?: stringResource(Res.string.settings_city_not_selected)) },
                )
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(
                    state.theme.icon(), stringResource(Res.string.settings_theme_row), stringResource(Res.string.settings_theme_row_description),
                    navigation.onTheme, iconColors = CoffeePeekIconBadgePalette.Violet,
                    trailing = { RowValue(stringResource(state.theme.valueLabel())) },
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            CoffeePeekSettingsSection(
                title = stringResource(Res.string.settings_section_other),
                description = stringResource(Res.string.settings_section_other_description),
            ) {
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Review, stringResource(Res.string.settings_support), stringResource(Res.string.settings_support_description),
                    external::openSupport, iconColors = CoffeePeekIconBadgePalette.Gold,
                )
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Lock, stringResource(Res.string.settings_policy_row), null, navigation.onUsagePolicy,
                    iconColors = CoffeePeekIconBadgePalette.Emerald,
                )
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(
                    CoffeePeekIcons.Share, stringResource(Res.string.settings_share), null, external::shareApp,
                    iconColors = CoffeePeekIconBadgePalette.BrightCyan,
                )
                StoreReviewRow(external)
            }
            SettingsUpdateSection(state, appInfo, onCheckForUpdates)
            Text(
                stringResource(Res.string.settings_version, appInfo.versionName), style = MaterialTheme.typography.labelSmall,
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
private fun SettingsUpdateSection(state: SettingsState, appInfo: SettingsAppInfo, onCheckForUpdates: () -> Unit) {
    if (!appInfo.canCheckForUpdates) return
    Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
    CoffeePeekSettingsSection(title = stringResource(Res.string.settings_section_update)) {
        val update = state.update
        CoffeePeekSettingsRow(
            CoffeePeekIcons.Info,
            stringResource(if (state.updateBusy) Res.string.settings_update_checking else Res.string.settings_update_check),
            if (update.updateAvailable) stringResource(Res.string.settings_update_available)
            else stringResource(Res.string.settings_version, appInfo.versionName),
            onCheckForUpdates,
            enabled = !state.updateBusy,
            trailing = if (state.updateBusy) {
                { CoffeePeekActivityIndicator(stringResource(Res.string.settings_loading), size = CoffeePeekDimens.settingsIconSize) }
            } else null,
        )
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
    ThemeMode.SYSTEM -> Res.string.settings_theme_value_system
    ThemeMode.LIGHT -> Res.string.settings_theme_value_light
    ThemeMode.DARK -> Res.string.settings_theme_value_dark
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
        CoffeePeekIcons.Star, stringResource(Res.string.settings_rate_app),
        stringResource(Res.string.settings_rate_app_description), external::rateApp,
        iconColors = CoffeePeekIconBadgePalette.Gold,
    )
}

@Preview
@Composable
private fun SettingsStoreReviewPreview() = CoffeePeekTheme {
    CoffeePeekSettingsSection(title = "CoffeePeek") {
        StoreReviewRow(object : SettingsExternalActions by previewExternal { override val canRateApp = true })
    }
}
private val previewSlots = SettingsSlots(guestCard = { _, _, modifier -> Text("Войти", modifier) }, notify = {})

@Preview
@Composable
private fun SettingsSignedInPreview() = CoffeePeekTheme {
    Surface {
        SettingsContent(SettingsState(true, ThemeMode.DARK, "Минск"), previewNavigation, previewExternal, previewSlots, SettingsAppInfo("1.0"), Dp.Hairline, {})
    }
}

@Preview
@Composable
private fun SettingsGuestPreview() = CoffeePeekTheme {
    Surface {
        SettingsContent(SettingsState(false, ThemeMode.SYSTEM), previewNavigation, previewExternal, previewSlots, SettingsAppInfo("1.0"), Dp.Hairline, {})
    }
}

@Composable
private fun SettingsDevelopmentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        SettingsContent(SettingsState(true, ThemeMode.SYSTEM), previewNavigation, previewExternal, previewSlots,
            SettingsAppInfo("1.0", canCheckForUpdates = false), Dp.Hairline, {})
    }
}

@Preview @Composable private fun SettingsDevelopmentLightPreview() = SettingsDevelopmentPreview(false)
@Preview @Composable private fun SettingsDevelopmentDarkPreview() = SettingsDevelopmentPreview(true)
