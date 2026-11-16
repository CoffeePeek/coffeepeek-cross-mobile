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

    fun observeUpdateMessage(onMessage: (IosSettingsUpdateMessage) -> Unit) =
        observe(presentation.updateMessages.map { it.toIos() }, onMessage)

    fun screenShown() = presentation.screenShown()
    fun checkForUpdates() = presentation.checkForUpdates()
    fun showUpdateMessage(message: String) = ErrorHandler.showError(message)
    fun setPushEnabled(enabled: Boolean) = notifications.setPushEnabled(enabled)
    fun refreshNotifications() = notifications.refresh()
    fun switchAccount() = perform { ownership.switchAccountMode() }

    fun open(action: IosSettingsAction) {
        when (action) {
            IosSettingsAction.City -> navigate(Navigator.Screen.CitySettings)
            IosSettingsAction.Language -> navigate(Navigator.Screen.LanguageSettings)
            IosSettingsAction.Theme -> navigate(Navigator.Screen.ThemeSettings)
            IosSettingsAction.Usage -> navigate(Navigator.Screen.UsagePolicy)
            IosSettingsAction.AddShop -> navigate(Navigator.Screen.AddShop)
            IosSettingsAction.AddRoaster -> navigate(Navigator.Screen.AddRoaster)
            IosSettingsAction.SignIn -> navigate(Navigator.Screen.Auth)
            IosSettingsAction.Register -> navigate(Navigator.Screen.Register)
            IosSettingsAction.Support -> external.openSupport()
            IosSettingsAction.Share -> external.shareApp()
            IosSettingsAction.Rate -> external.rateApp()
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

enum class IosSettingsAction { City, Language, Theme, Usage, AddShop, AddRoaster, SignIn, Register, Support, Share, Rate }
enum class IosSettingsUpdateMessage { UpToDate, Failed }

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

private fun NativeSettingsUpdateMessage.toIos() = when (this) {
    NativeSettingsUpdateMessage.UpToDate -> IosSettingsUpdateMessage.UpToDate
    NativeSettingsUpdateMessage.Failed -> IosSettingsUpdateMessage.Failed
}
