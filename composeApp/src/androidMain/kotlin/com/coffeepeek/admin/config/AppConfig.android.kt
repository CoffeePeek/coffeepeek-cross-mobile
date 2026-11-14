package com.coffeepeek.admin.config

import com.coffeepeek.BuildConfig

actual object AppConfig {
    actual val versionCode: Long?
        get() {
            val context = com.coffeepeek.admin.locator.Locator.appContext
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)
        }
    actual val updatePlatform: String = "android"
    actual val updateChannel: String? = if (BuildConfig.APK_UPDATES_ENABLED) "apk" else "play"
    actual val versionName: String = BuildConfig.VERSION_NAME
    actual val baseUrl: String = BuildConfig.API_BASE_URL
    actual val isDebug: Boolean = BuildConfig.DEBUG
}
