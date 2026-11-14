package com.coffeepeek.admin.utils

import android.content.Intent
import androidx.core.net.toUri
import com.coffeepeek.admin.locator.Locator

actual object OpenInBrowser {

    actual fun openInBrowser(link: String) {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Locator.appContext.startActivity(intent)
    }
}