package com.coffeepeek.admin.location

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.coffeepeek.admin.CoffeePeekApplication

internal actual fun openAppLocationSettings() {
    val context = CoffeePeekApplication.context
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
