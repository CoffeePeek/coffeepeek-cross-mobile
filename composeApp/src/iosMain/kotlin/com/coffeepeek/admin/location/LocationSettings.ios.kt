package com.coffeepeek.admin.location

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal actual fun openAppLocationSettings() {
    NSURL(string = UIApplication.sharedApplication.openSettingsURLString)
        .let { UIApplication.sharedApplication.openURL(it) }
}
