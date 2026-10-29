package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppVersionFooter
import com.coffeepeek.admin.ui.component.PlatformActivityIndicator
import com.coffeepeek.admin.ui.component.GuestAuthCard
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.SettingsDivider
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.component.SettingsRow
import com.coffeepeek.admin.ui.component.SettingsSection
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.COFFEEPEEK_SHARE_TEXT
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.appReviewUrl
import com.coffeepeek.admin.config.AppConfig
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.settings_rate_app
import coffeepeek.composeapp.generated.resources.settings_rate_app_description
import coffeepeek.composeapp.generated.resources.settings_other_description
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(vm: ProfileViewModel = koinInject()) {
    val profile by vm.uiState.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val cities by vm.cities.collectAsState()
    val selectedCityId by vm.selectedCityId.collectAsState()
    val updates = koinInject<com.coffeepeek.feature.appupdate.impl.AppUpdateController>()
    val updateState by updates.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        PlatformScrollableColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                ,
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CpDimens.settingsPagePadding,
                        vertical = CpDimens.spacing4,
                    ),
            )

            if (!profile.isLoggedIn) {
                GuestAuthCard(
                    onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
                    onRegister = { Navigator.navigate(Navigator.Screen.Register) },
                    modifier = Modifier.padding(horizontal = CpDimens.settingsPagePadding),
                )
                Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
            }

            koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().AccountSwitch()
            SettingsSection(title = "Добавить") {
                SettingsRow(
                    icon = CpIcons.Add,
                    label = "Добавить кофейню",
                    description = "Предложить новое место для CoffeePeek",
                    iconColors = SettingsIconPalette.Gold,
                    onClick = { Navigator.navigate(Navigator.Screen.AddShop) },
                )
                SettingsDivider()
                SettingsRow(
                    icon = CpIcons.CoffeeBean,
                    label = "Добавить обжарщика",
                    description = "Помогите сообществу открыть новых обжарщиков",
                    iconColors = SettingsIconPalette.Mint,
                    onClick = { Navigator.navigate(Navigator.Screen.AddRoaster) },
                )
            }

            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))

            SettingsSection(title = "Настройки") {
                SettingsRow(
                    icon = CpIcons.Location,
                    label = "Город",
                    description = "Определяет, какие кофейни показывать в первую очередь",
                    iconColors = SettingsIconPalette.Aqua,
                    trailing = {
                        CityValue(
                            cityName = cities.firstOrNull { it.id == selectedCityId }?.name,
                        )
                    },
                    onClick = { Navigator.navigate(Navigator.Screen.CitySettings) },
                )
                SettingsDivider()
                SettingsRow(
                    icon = themeMode.icon(),
                    label = "Тема",
                    description = "Настройте внешний вид приложения",
                    iconColors = SettingsIconPalette.Violet,
                    trailing = { ThemeValue(themeMode) },
                    onClick = { Navigator.navigate(Navigator.Screen.ThemeSettings) },
                )
            }

            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))

            SettingsSection(
                title = "Другие настройки",
                description = stringResource(Res.string.settings_other_description),
            ) {
                SettingsRow(
                    icon = CpIcons.Review,
                    label = "Связаться с поддержкой",
                    description = "Поможем с вопросами и проблемами",
                    iconColors = SettingsIconPalette.Gold,
                    onClick = { OpenInBrowser.openInBrowser("https://coffeepeek.by/support") },
                )
                SettingsDivider()
                SettingsRow(
                    icon = CpIcons.Lock,
                    label = "Политика использования",
                    iconColors = SettingsIconPalette.Emerald,
                    onClick = { Navigator.navigate(Navigator.Screen.UsagePolicy) },
                )
                SettingsDivider()
                SettingsRow(
                    icon = CpIcons.Share,
                    label = "Поделиться",
                    iconColors = SettingsIconPalette.BrightCyan,
                    onClick = { ShareHelper.shareText(COFFEEPEEK_SHARE_TEXT) },
                )
                val reviewUrl = appReviewUrl(AppConfig.updatePlatform, AppConfig.updateChannel)
                StoreReviewRow(reviewUrl?.let { url -> { OpenInBrowser.openInBrowser(url) } })
            }

            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
            SettingsSection(title = "Обновление приложения") {
                SettingsRow(
                    icon = CpIcons.Info,
                    label = if (updateState.checking) "Проверяем обновления…" else "Проверить обновления",
                    description = updateState.message ?: if (updateState.update != null) "Доступна новая версия CoffeePeek" else "Версия ${com.coffeepeek.admin.config.AppConfig.versionName}",
                    onClick = { updates.check(manual = true) },
                    enabled = !updateState.checking,
                    trailing = if (updateState.checking) {
                        { PlatformActivityIndicator(Modifier.size(CpDimens.settingsIconSize)) }
                    } else null,
                )
            }
            AppVersionFooter()
            Spacer(Modifier.height(CpDimens.spacing8 + LocalFloatingNavClearance.current))
        }
    }
}

@Composable
private fun StoreReviewRow(onReview: (() -> Unit)?) {
    if (onReview == null) return
    SettingsDivider()
    SettingsRow(
        icon = CpIcons.Star,
        label = stringResource(Res.string.settings_rate_app),
        description = stringResource(Res.string.settings_rate_app_description),
        iconColors = SettingsIconPalette.Gold,
        onClick = onReview,
    )
}

@Preview
@Composable
private fun StoreReviewRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SettingsSection(title = "CoffeePeek") { StoreReviewRow({}) }
}

@Preview
@Composable
private fun StoreReviewRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SettingsSection(title = "CoffeePeek") { StoreReviewRow({}) }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "Авто"
    ThemeMode.LIGHT -> "Светлая"
    ThemeMode.DARK -> "Тёмная"
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> CpIcons.ThemeSystem
    ThemeMode.LIGHT -> CpIcons.ThemeLight
    ThemeMode.DARK -> CpIcons.ThemeDark
}

@Composable
private fun CityValue(cityName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Text(
            text = cityName ?: "Выберите",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = CpIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(CpDimens.settingsIconSize),
        )
    }
}

@Composable
private fun ThemeValue(current: ThemeMode) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Text(
            text = current.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Icon(
            imageVector = CpIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(CpDimens.settingsIconSize),
        )
    }
}
