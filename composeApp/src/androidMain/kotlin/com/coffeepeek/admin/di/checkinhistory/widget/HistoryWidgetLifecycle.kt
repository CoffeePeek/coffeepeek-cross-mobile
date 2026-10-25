package com.coffeepeek.admin.di.checkinhistory.widget

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** Returning to the launcher refreshes completed edits from either history source. */
internal class HistoryWidgetLifecycle : Application.ActivityLifecycleCallbacks {
    override fun onActivityStopped(activity: Activity) { ActivityCalendarWidgetRuntime.refresh() }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
