package com.coffeepeek.admin.di.checkinhistory.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.coffeepeek.admin.ui.Navigator

private const val OPEN_HISTORY = "com.coffeepeek.widget.OPEN_HISTORY"

internal fun historyWidgetOpenIntent(context: Context): PendingIntent? {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    intent.action = OPEN_HISTORY
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

internal fun historyWidgetRefreshIntent(context: Context): PendingIntent {
    val intent = Intent(context, ActivityCalendarWidgetProvider::class.java).setAction(ActivityCalendarWidgetProvider.REFRESH)
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

internal fun openHistoryWidgetIntent(intent: Intent) {
    if (intent.action == OPEN_HISTORY) Navigator.openAppLink(Navigator.Screen.VisitedPlaces)
}
