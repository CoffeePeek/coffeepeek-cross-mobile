package com.coffeepeek.admin.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object OpenInBrowser {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    actual fun openInBrowser(link: String) {
        val url = NSURL.URLWithString(link.trim())
        if (url == null) {
            ErrorHandler.showError("Некорректная ссылка")
            return
        }
        mainScope.launch {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { opened ->
                if (!opened) ErrorHandler.showError("Не удалось открыть ссылку")
            }
        }
    }
}
