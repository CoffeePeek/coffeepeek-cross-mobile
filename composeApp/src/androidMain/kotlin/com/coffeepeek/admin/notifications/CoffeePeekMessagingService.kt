package com.coffeepeek.admin.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CoffeePeekMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { AndroidPush.refresh() }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!NativePushBridge.accepts(message.data["userId"]) || !AndroidPush.permitted()) return
        val notice = message.notification ?: return
        val id = message.data["notificationId"] ?: message.messageId ?: return
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, AndroidPush.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_coffee).setContentTitle(notice.title).setContentText(notice.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.body)).setAutoCancel(true).setContentIntent(pending).build()
        try { NotificationManagerCompat.from(this).notify(id, 0, notification) } catch (_: SecurityException) { }
    }
}
