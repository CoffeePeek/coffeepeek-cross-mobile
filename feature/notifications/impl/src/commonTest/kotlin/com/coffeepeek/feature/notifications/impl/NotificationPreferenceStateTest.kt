package com.coffeepeek.feature.notifications.impl

import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationSettingsError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferenceStateTest {
    private val ready = NotificationInboxState(accountId = "account", settingsLoaded = true,
        deviceAvailable = true, permissionGranted = true, pushEnabled = true)

    @Test fun aGuestHasNoActiveSwitchEvenIfOldPermissionAndPreferenceValuesArePresent() {
        val guest = ready.copy(accountId = null)
        assertFalse(guest.canChangePushPreference)
        assertFalse(guest.pushPreferenceChecked)
    }

    @Test fun aStoredMasterPreferenceAndSystemPermissionBothContributeToTheDisplayedValue() {
        assertTrue(ready.canChangePushPreference)
        assertTrue(ready.pushPreferenceChecked)
        assertFalse(ready.copy(pushEnabled = false).pushPreferenceChecked)
        val denied = ready.copy(permissionGranted = false)
        assertFalse(denied.pushPreferenceChecked)
        assertTrue(denied.canChangePushPreference, "The existing explicit toggle still offers permission/settings")
    }

    @Test fun loadingUnavailableAndSavingDisableChangesButAFailedSaveAllowsRetry() {
        assertFalse(ready.copy(settingsLoaded = false).canChangePushPreference)
        assertFalse(ready.copy(deviceAvailable = false).canChangePushPreference)
        assertFalse(ready.copy(savingSettings = true).canChangePushPreference)
        assertFalse(ready.copy(settingsLoaded = false, settingsError = NotificationSettingsError.LoadFailed).canChangePushPreference)
        assertTrue(ready.copy(settingsError = NotificationSettingsError.SaveFailed).canChangePushPreference)
    }
}
