package com.coffeepeek.admin.notifications

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.coffeepeek.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CoffeePeekMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { AndroidPush.refresh() }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!NativePushBridge.accepts(message.data["userId"]) || !AndroidPush.permitted()) return
        NativePushBridge.notificationReceived(message.data["userId"])
        val notice = message.notification ?: return
        val id = message.data["notificationId"] ?: message.messageId ?: return
        val intent = notificationLaunchIntent(this, message.data)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, AndroidPush.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_coffee_bean).setContentTitle(notice.title).setContentText(notice.body)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.body)).setAutoCancel(true).setContentIntent(pending).build()
        try { NotificationManagerCompat.from(this).notify(id, 0, notification) } catch (_: SecurityException) { }
    }
}
