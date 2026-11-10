package com.coffeepeek.admin.di.appupdate

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.api.DismissedUpdateStore
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository

/** The installed version, platform and update channel come from the build configuration. */
object AppConfigUpdateEnvironment : AppUpdateEnvironment {
    override val checksEnabled: Boolean get() = appUpdateChecksEnabled(AppConfig.updatePlatform, AppConfig.isDebug)
    override val versionCode: Long? get() = AppConfig.versionCode
    override val platform: String get() = AppConfig.updatePlatform
    override val channel: String? get() = AppConfig.updateChannel
}

/** A local iOS development app is not an App Store installation. Android/Sandbox policies stay explicit. */
internal fun appUpdateChecksEnabled(platform: String, debug: Boolean): Boolean = platform != "ios" || !debug

/** Keeps the dismissed version in the app's settings table under the key earlier releases already use. */
class SettingsDismissedUpdateStore(private val settings: SettingRepository) : DismissedUpdateStore {
    override suspend fun read(): Long? = settings.read(KEY)?.value?.toLongOrNull()
    override suspend fun write(version: Long) = settings.save(Setting(KEY, version.toString()))

    companion object {
        const val KEY = "dismissed_app_version"
    }
}
