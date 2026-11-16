package com.coffeepeek.admin.di.settings

import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationSettingsError
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.ownership.api.BusinessSessionState
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.SettingsAppInfo
import com.coffeepeek.feature.settings.api.SettingsExternalActions
import com.coffeepeek.feature.settings.impl.NativeSettingsTab
import com.coffeepeek.feature.settings.impl.NativeSettingsTabState
import com.coffeepeek.feature.settings.impl.NativeSettingsUpdateMessage
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class IosSettingsTab internal constructor(
    private val presentation: NativeSettingsTab,
    language: LanguagePort,
    private val notifications: NotificationFeature,
    private val ownership: BusinessOwnershipEntry,
    private val external: SettingsExternalActions,
    private val appInfo: SettingsAppInfo,
    private val navigate: (Navigator.Screen) -> Unit,
) : IosSettingsScreen(language, {}, presentation::close) {
    fun currentState() = snapshot(presentation.current(), notifications.state.value, ownership.accountSwitchState.value)

    fun observeState(onState: (IosSettingsTabState) -> Unit) =
        observe(combine(presentation.state, notifications.state, ownership.accountSwitchState, ::snapshot).distinctUntilChanged(), onState)

    fun observeNotificationRefresh(): IosSettingsObservation =
        observe(notifications.state.map { it.generation }.distinctUntilChanged()) { notifications.refresh() }

    fun observeUpdateMessage(onMessageKey: (String) -> Unit) =
        observe(presentation.updateMessages.map { it.messageKey() }, onMessageKey)

    fun screenShown() = presentation.screenShown()
    fun checkForUpdates() = presentation.checkForUpdates()
    fun showUpdateMessage(message: String) = ErrorHandler.showError(message)
    fun setPushEnabled(enabled: Boolean) = notifications.setPushEnabled(enabled)
    fun refreshNotifications() = notifications.refresh()
    fun switchAccount() = perform { ownership.switchAccountMode() }

    fun open(action: IosSettingsAction) {
        when (action) {
            IosSettingsAction.CITY -> navigate(Navigator.Screen.CitySettings)
            IosSettingsAction.LANGUAGE -> navigate(Navigator.Screen.LanguageSettings)
            IosSettingsAction.THEME -> navigate(Navigator.Screen.ThemeSettings)
            IosSettingsAction.USAGE -> navigate(Navigator.Screen.UsagePolicy)
            IosSettingsAction.SHOP -> navigate(Navigator.Screen.AddShop)
            IosSettingsAction.ROASTER -> navigate(Navigator.Screen.AddRoaster)
            IosSettingsAction.LOGIN -> navigate(Navigator.Screen.Auth)
            IosSettingsAction.REGISTRATION -> navigate(Navigator.Screen.Register)
            IosSettingsAction.SUPPORT -> external.openSupport()
            IosSettingsAction.SHARE -> external.shareApp()
            IosSettingsAction.RATE -> external.rateApp()
        }
    }

    private fun snapshot(state: NativeSettingsTabState, notifications: NotificationInboxState, ownership: BusinessSessionState) =
        IosSettingsTabState(
            showGuestCard = state.showGuestCard,
            showAddRows = state.showAddRows,
            city = state.city,
            theme = state.theme.toApp(),
            languageTag = state.languageTag,
            checkingUpdates = state.checkingUpdates,
            updateAvailable = state.updateAvailable,
            updateChecksEnabled = appInfo.canCheckForUpdates,
            version = appInfo.versionName,
            canRateApp = external.canRateApp,
            hasBusinesses = ownership.hasBusinesses,
            businessMode = ownership.businessMode,
            notifications = notifications.toIos(),
        )
}

enum class IosSettingsAction { CITY, LANGUAGE, THEME, USAGE, SHOP, ROASTER, LOGIN, REGISTRATION, SUPPORT, SHARE, RATE }

data class IosSettingsTabState(
    val showGuestCard: Boolean,
    val showAddRows: Boolean,
    val city: String?,
    val theme: ThemeMode,
    val languageTag: String,
    val checkingUpdates: Boolean,
    val updateAvailable: Boolean,
    val updateChecksEnabled: Boolean,
    val version: String,
    val canRateApp: Boolean,
    val hasBusinesses: Boolean,
    val businessMode: Boolean,
    val notifications: IosNotificationSettingsState,
)

data class IosNotificationSettingsState(
    val guest: Boolean,
    val checked: Boolean,
    val enabled: Boolean,
    val descriptionKey: String,
    val failureKey: String?,
    val canRetry: Boolean,
)

private fun NotificationInboxState.toIos() = IosNotificationSettingsState(
    guest = accountId == null,
    checked = pushPreferenceChecked,
    enabled = canChangePushPreference,
    descriptionKey = when {
        accountId == null -> "notifications_settings_sign_in_description"
        !deviceAvailable -> "notifications_push_unavailable"
        savingSettings -> "notifications_saving"
        !settingsLoaded -> "notifications_loading"
        !permissionGranted -> "notifications_push_permission"
        else -> "settings_notifications_description"
    },
    failureKey = when (settingsError) {
        NotificationSettingsError.LoadFailed -> "notifications_push_load_failed"
        NotificationSettingsError.SaveFailed -> "notifications_push_save_failed"
        null -> null
    },
    canRetry = !settingsLoaded && settingsError != null,
)

private fun NativeSettingsUpdateMessage.messageKey() = when (this) {
    NativeSettingsUpdateMessage.UpToDate -> "settings_update_up_to_date"
    NativeSettingsUpdateMessage.Failed -> "settings_update_failed"
}
