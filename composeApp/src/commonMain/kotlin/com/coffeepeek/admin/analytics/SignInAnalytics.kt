package com.coffeepeek.admin.analytics

internal fun signInAnalytics(userId: String?, method: String, entry: String) {
    if (userId == null) return
    Analytics.service?.syncIdentityNow(userId)
    Analytics.track(AnalyticsEvent.SIGN_IN_COMPLETED, "method" to method, "entry_point" to entry)
}
