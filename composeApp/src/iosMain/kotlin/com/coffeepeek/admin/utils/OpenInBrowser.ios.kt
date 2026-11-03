package com.coffeepeek.admin.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.native_link_invalid
import coffeepeek.composeapp.generated.resources.native_link_open_failed
import org.jetbrains.compose.resources.getString

actual object OpenInBrowser {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    actual fun openInBrowser(link: String) {
        mainScope.launch {
            val url = NSURL.URLWithString(link.trim())
            if (url == null) {
                ErrorHandler.showError(getString(Res.string.native_link_invalid))
                return@launch
            }
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { opened ->
                if (!opened) mainScope.launch { ErrorHandler.showError(getString(Res.string.native_link_open_failed)) }
            }
        }
    }
}
