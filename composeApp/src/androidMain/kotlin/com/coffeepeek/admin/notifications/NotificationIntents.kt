package com.coffeepeek.admin.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.coffeepeek.admin.MainActivity

internal fun notificationLaunchIntent(context: Context, data: Map<String, String>): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .setData(Uri.Builder().scheme("coffeepeek").authority("notifications")
            .appendPath(data["notificationId"].orEmpty()).build())
        .putExtra("notificationId", data["notificationId"])
        .putExtra("userId", data["userId"])

internal fun openNotificationIntent(intent: Intent) {
    NotificationOpens.submit(mapOf("notificationId" to intent.getStringExtra("notificationId"),
        "userId" to intent.getStringExtra("userId")))
}
