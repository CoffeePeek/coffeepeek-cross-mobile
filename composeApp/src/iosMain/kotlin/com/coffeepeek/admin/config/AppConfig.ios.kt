package com.coffeepeek.admin.config

import platform.Foundation.NSBundle
import platform.Foundation.NSNumber

actual object AppConfig {
    actual val versionCode: Long?
        get() = bundleString("CFBundleVersion").toLongOrNull()?.takeIf { it >= 0 }
    actual val updatePlatform: String = "ios"
    actual val updateChannel: String? = null
    actual val versionName: String
        get() = bundleString("CFBundleShortVersionString").ifBlank { "1.0" }

    actual val baseUrl: String
        get() = bundleString("APIBaseURL").trim()

    actual val isDebug: Boolean
        get() = (NSBundle.mainBundle.objectForInfoDictionaryKey("CoffeePeekDebug") as? NSNumber)
            ?.boolValue
            ?: bundleString("CoffeePeekDebug").equals("YES", ignoreCase = true)

    private fun bundleString(key: String): String =
        NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""
}
