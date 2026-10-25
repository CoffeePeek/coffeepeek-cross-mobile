package com.coffeepeek.admin.di.checkinhistory.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class ActivityCalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ActivityCalendarWidgetRuntime.refresh(clear = true)
    }

    // Android selects the supplied responsive RemoteViews during resize without clearing photos or starting a read.

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(REFRESH, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_DATE_CHANGED)) ActivityCalendarWidgetRuntime.refresh(clear = true)
    }

    override fun onDisabled(context: Context) { ActivityCalendarWidgetRuntime.cancel() }

    companion object {
        internal const val REFRESH = "com.coffeepeek.widget.REFRESH"
        internal fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, ActivityCalendarWidgetProvider::class.java))
    }
}
