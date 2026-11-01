package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.settings.AppLanguagePreference
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.COFFEEPEEK_SHARE_TEXT
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.appReviewUrl
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.appupdate.impl.UpdateUiState
import com.coffeepeek.feature.appupdate.impl.UpdateCheckFeedback
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(vm: ProfileViewModel = koinInject()) {
    val profile by vm.uiState.collectAsState()
    val theme by vm.themeMode.collectAsState()
    val cities by vm.cities.collectAsState()
    val cityId by vm.selectedCityId.collectAsState()
    val updates = koinInject<AppUpdateController>()
    val update by updates.state.collectAsState()
    val language by koinInject<AppLanguagePreference>().selection.collectAsState()
    val ownership = koinInject<BusinessOwnershipEntry>()
    val state = LegacySettingsState(profile.isLoggedIn, theme, cities.firstOrNull { it.id == cityId }?.name,
        language ?: AppLanguage.RUSSIAN, update.checking, update.update != null, legacyUpdateMessage(update), AppConfig.versionName)
    SettingsScreenContent(state, legacySettingsActions(updates), bottomClearance = LocalFloatingNavClearance.current,
        accountSwitch = { ownership.AccountSwitch() })
}

internal data class LegacySettingsState(
    val signedIn: Boolean, val theme: ThemeMode, val cityName: String?, val language: AppLanguage,
    val checking: Boolean = false, val updateAvailable: Boolean = false, val updateMessage: String? = null, val version: String,
)

internal class LegacySettingsActions(
    val addShop: () -> Unit, val addRoaster: () -> Unit, val city: () -> Unit, val theme: () -> Unit,
    val language: () -> Unit, val notifications: () -> Unit, val policy: () -> Unit, val login: () -> Unit, val register: () -> Unit,
    val support: () -> Unit, val share: () -> Unit, val checkUpdate: () -> Unit, val review: (() -> Unit)?,
)

private fun legacySettingsActions(updates: AppUpdateController): LegacySettingsActions {
    val reviewUrl = appReviewUrl(AppConfig.updatePlatform, AppConfig.updateChannel)
    return LegacySettingsActions(
        addShop = { Navigator.navigate(Navigator.Screen.AddShop) },
        addRoaster = { Navigator.navigate(Navigator.Screen.AddRoaster) },
        city = { Navigator.navigate(Navigator.Screen.CitySettings) },
        theme = { Navigator.navigate(Navigator.Screen.ThemeSettings) },
        language = { Navigator.navigate(Navigator.Screen.LanguageSettings) },
        notifications = { Navigator.navigate(Navigator.Screen.NotificationSettings) },
        policy = { Navigator.navigate(Navigator.Screen.UsagePolicy) },
        login = { Navigator.navigate(Navigator.Screen.Auth) }, register = { Navigator.navigate(Navigator.Screen.Register) },
        support = { OpenInBrowser.openInBrowser("https://coffeepeek.by/support") },
        share = { ShareHelper.shareText(COFFEEPEEK_SHARE_TEXT) }, checkUpdate = { updates.check(manual = true) },
        review = reviewUrl?.let { url -> { OpenInBrowser.openInBrowser(url) } },
    )
}

@Composable
private fun legacyUpdateMessage(state: UpdateUiState): String? {
    return when (state.feedback) {
        null -> null
        UpdateCheckFeedback.CheckFailed -> stringResource(Res.string.legacy_settings_update_failed)
        UpdateCheckFeedback.UpToDate -> stringResource(Res.string.legacy_settings_update_up_to_date)
    }
}

@Composable
internal fun SettingsScreenContent(
    state: LegacySettingsState, actions: LegacySettingsActions, bottomClearance: Dp,
    accountSwitch: @Composable () -> Unit = {}, copy: LegacySettingsCopy = legacySettingsCopy(state),
) {
    CoffeePeekScaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
        PlatformScrollableColumn(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(copy.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CpDimens.settingsPagePadding, vertical = CpDimens.spacing4))
            if (!state.signedIn) {
                GuestAuthCard(actions.login, actions.register, Modifier.padding(horizontal = CpDimens.settingsPagePadding))
                Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
            }
            accountSwitch()
            LegacySettingsAdd(copy, actions)
            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
            LegacySettingsPreferences(state, copy, actions)
            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
            LegacySettingsOther(copy, actions)
            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
            LegacySettingsUpdate(state, copy, actions.checkUpdate)
            Text(copy.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = CpDimens.settingsPagePadding, top = CpDimens.spacing4,
                    end = CpDimens.settingsPagePadding))
            Spacer(Modifier.height(CpDimens.spacing8 + bottomClearance))
        }
    }
}

@Composable
private fun LegacySettingsAdd(copy: LegacySettingsCopy, actions: LegacySettingsActions) {
    SettingsSection(title = copy.add) {
        SettingsRow(CpIcons.Add, copy.addShop, copy.addShopDescription, iconColors = SettingsIconPalette.Gold, onClick = actions.addShop)
        SettingsDivider()
        SettingsRow(CpIcons.CoffeeBean, copy.addRoaster, copy.addRoasterDescription, iconColors = SettingsIconPalette.Mint, onClick = actions.addRoaster)
    }
}

@Composable
private fun LegacySettingsPreferences(state: LegacySettingsState, copy: LegacySettingsCopy, actions: LegacySettingsActions) {
    SettingsSection(title = copy.title) {
        SettingsRow(CpIcons.Location, copy.city, copy.cityDescription, iconColors = SettingsIconPalette.Aqua,
            trailing = { LegacySettingsValue(state.cityName ?: copy.chooseCity) }, onClick = actions.city)
        SettingsDivider()
        SettingsRow(state.theme.icon(), copy.theme, copy.themeDescription, iconColors = SettingsIconPalette.Violet,
            trailing = { LegacySettingsValue(copy.themeValue) }, onClick = actions.theme)
        SettingsDivider()
        SettingsRow(CpIcons.Globe, copy.language, copy.languageDescription, iconColors = SettingsIconPalette.Mint,
            trailing = { LegacySettingsValue(copy.languageValue) }, onClick = actions.language)
        SettingsDivider()
        SettingsRow(CpIcons.Notifications, copy.notifications, copy.notificationsDescription,
            iconColors = SettingsIconPalette.Gold, onClick = actions.notifications)
    }
}

@Composable
private fun LegacySettingsOther(copy: LegacySettingsCopy, actions: LegacySettingsActions) {
    SettingsSection(title = copy.other, description = copy.otherDescription) {
        SettingsRow(CpIcons.Review, copy.support, copy.supportDescription, iconColors = SettingsIconPalette.Gold, onClick = actions.support)
        SettingsDivider()
        SettingsRow(CpIcons.Lock, copy.policy, iconColors = SettingsIconPalette.Emerald, onClick = actions.policy)
        SettingsDivider()
        SettingsRow(CpIcons.Share, copy.share, iconColors = SettingsIconPalette.BrightCyan, onClick = actions.share)
        actions.review?.let { onReview ->
            SettingsDivider()
            SettingsRow(CpIcons.Star, copy.rate, copy.rateDescription, iconColors = SettingsIconPalette.Gold, onClick = onReview)
        }
    }
}

@Composable
private fun LegacySettingsUpdate(state: LegacySettingsState, copy: LegacySettingsCopy, onCheck: () -> Unit) {
    SettingsSection(title = copy.updates) {
        SettingsRow(CpIcons.Info, if (state.checking) copy.checking else copy.checkUpdates,
            state.updateMessage ?: if (state.updateAvailable) copy.availableUpdate else copy.version,
            onClick = onCheck, enabled = !state.checking,
            trailing = if (state.checking) { { PlatformActivityIndicator(Modifier.size(CpDimens.settingsIconSize)) } } else null)
    }
}

@Composable
private fun LegacySettingsValue(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(CpIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(CpDimens.settingsIconSize))
    }
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> CpIcons.ThemeSystem
    ThemeMode.LIGHT -> CpIcons.ThemeLight
    ThemeMode.DARK -> CpIcons.ThemeDark
}

private fun ThemeMode.valueResource() = when (this) {
    ThemeMode.SYSTEM -> Res.string.legacy_settings_theme_auto
    ThemeMode.LIGHT -> Res.string.legacy_settings_theme_light
    ThemeMode.DARK -> Res.string.legacy_settings_theme_dark
}

internal data class LegacySettingsCopy(
    val title: String, val add: String, val addShop: String, val addShopDescription: String, val addRoaster: String,
    val addRoasterDescription: String, val city: String, val cityDescription: String, val chooseCity: String,
    val theme: String, val themeDescription: String, val themeValue: String, val language: String,
    val languageDescription: String, val languageValue: String, val notifications: String, val notificationsDescription: String,
    val other: String, val otherDescription: String,
    val support: String, val supportDescription: String, val policy: String, val share: String, val rate: String,
    val rateDescription: String, val updates: String, val checking: String, val checkUpdates: String,
    val availableUpdate: String, val version: String,
)

@Composable
private fun legacySettingsCopy(state: LegacySettingsState) = LegacySettingsCopy(
    title = stringResource(Res.string.legacy_settings_title), add = stringResource(Res.string.legacy_settings_add),
    addShop = stringResource(Res.string.legacy_settings_add_shop), addShopDescription = stringResource(Res.string.legacy_settings_add_shop_description),
    addRoaster = stringResource(Res.string.legacy_settings_add_roaster), addRoasterDescription = stringResource(Res.string.legacy_settings_add_roaster_description),
    city = stringResource(Res.string.legacy_settings_city), cityDescription = stringResource(Res.string.legacy_settings_city_description),
    chooseCity = stringResource(Res.string.legacy_settings_city_choose), theme = stringResource(Res.string.legacy_settings_theme),
    themeDescription = stringResource(Res.string.legacy_settings_theme_description), themeValue = stringResource(state.theme.valueResource()),
    language = stringResource(Res.string.legacy_settings_language), languageDescription = stringResource(Res.string.legacy_settings_language_description),
    languageValue = stringResource(if (state.language == AppLanguage.RUSSIAN) Res.string.legacy_settings_language_russian else Res.string.legacy_settings_language_belarusian),
    notifications = stringResource(Res.string.legacy_settings_notifications),
    notificationsDescription = stringResource(Res.string.legacy_settings_notifications_description),
    other = stringResource(Res.string.legacy_settings_other), otherDescription = stringResource(Res.string.settings_other_description),
    support = stringResource(Res.string.legacy_settings_support), supportDescription = stringResource(Res.string.legacy_settings_support_description),
    policy = stringResource(Res.string.legacy_settings_policy), share = stringResource(Res.string.legacy_settings_share),
    rate = stringResource(Res.string.settings_rate_app), rateDescription = stringResource(Res.string.settings_rate_app_description),
    updates = stringResource(Res.string.legacy_settings_update_section), checking = stringResource(Res.string.legacy_settings_update_checking),
    checkUpdates = stringResource(Res.string.legacy_settings_update_check), availableUpdate = stringResource(Res.string.legacy_settings_update_available),
    version = stringResource(Res.string.legacy_settings_version, state.version),
)

private val russianSettingsPreviewCopy = LegacySettingsCopy(
    title = "Настройки", add = "Добавить", addShop = "Добавить кофейню", addShopDescription = "Предложить новое место для CoffeePeek",
    addRoaster = "Добавить обжарщика", addRoasterDescription = "Помогите сообществу открыть новых обжарщиков",
    city = "Город", cityDescription = "Определяет, какие кофейни показывать в первую очередь", chooseCity = "Выберите",
    theme = "Тема", themeDescription = "Настройте внешний вид приложения", themeValue = "Тёмная",
    language = "Язык", languageDescription = "Язык интерфейса приложения", languageValue = "Русский",
    notifications = "Уведомления", notificationsDescription = "Включайте и выключайте уведомления приложения",
    other = "Другие настройки", otherDescription = "Полезные дополнения, обратная связь и конфиденциальность",
    support = "Связаться с поддержкой", supportDescription = "Поможем с вопросами и проблемами", policy = "Политика использования",
    share = "Поделиться", rate = "Оценить приложение", rateDescription = "Оставьте отзыв в магазине приложений",
    updates = "Обновление приложения", checking = "Проверяем обновления…", checkUpdates = "Проверить обновления",
    availableUpdate = "Доступна новая версия CoffeePeek", version = "Версия 1.0",
)
private val belarusianSettingsPreviewCopy = LegacySettingsCopy(
    title = "Налады", add = "Дадаць", addShop = "Дадаць кавярню", addShopDescription = "Прапанаваць новае месца для CoffeePeek",
    addRoaster = "Дадаць абсмажчыка", addRoasterDescription = "Дапамажыце супольнасці адкрыць новых абсмажчыкаў",
    city = "Горад", cityDescription = "Вызначае, якія кавярні паказваць у першую чаргу", chooseCity = "Абярыце",
    theme = "Тэма", themeDescription = "Наладзьце выгляд праграмы", themeValue = "Цёмная",
    language = "Мова", languageDescription = "Мова інтэрфейсу праграмы", languageValue = "Беларуская",
    notifications = "Апавяшчэнні", notificationsDescription = "Уключайце і выключайце апавяшчэнні праграмы",
    other = "Іншыя налады", otherDescription = "Карысныя дапаўненні, зваротная сувязь і канфідэнцыйнасць",
    support = "Звязацца з падтрымкай", supportDescription = "Дапаможам з пытаннямі і праблемамі", policy = "Палітыка выкарыстання",
    share = "Падзяліцца", rate = "Ацаніць праграму", rateDescription = "Пакіньце водгук у краме праграм",
    updates = "Абнаўленне праграмы", checking = "Правяраем абнаўленні…", checkUpdates = "Праверыць абнаўленні",
    availableUpdate = "Даступная новая версія CoffeePeek", version = "Версія 1.0",
)
private val settingsPreviewActions = LegacySettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@Composable
private fun LegacySettingsPreview(dark: Boolean, belarusian: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val state = LegacySettingsState(true, ThemeMode.DARK, if (belarusian) "Мінск" else "Минск",
        if (belarusian) AppLanguage.BELARUSIAN else AppLanguage.RUSSIAN, version = "1.0")
    SettingsScreenContent(state, settingsPreviewActions, 0.dp,
        copy = if (belarusian) belarusianSettingsPreviewCopy else russianSettingsPreviewCopy)
}

@Preview @Composable private fun LegacySettingsRussianLightPreview() = LegacySettingsPreview(false)
@Preview @Composable private fun LegacySettingsRussianDarkPreview() = LegacySettingsPreview(true)
@Preview @Composable private fun LegacySettingsBelarusianLightPreview() = LegacySettingsPreview(false, true)
@Preview @Composable private fun LegacySettingsBelarusianDarkPreview() = LegacySettingsPreview(true, true)
