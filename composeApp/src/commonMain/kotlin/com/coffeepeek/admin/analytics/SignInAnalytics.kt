package com.coffeepeek.admin.analytics

internal fun signInAnalytics(userId: String?, method: String, entry: String, email: String? = null) {
    if (userId == null) return
    Analytics.service?.syncIdentityNow(userId)
    Analytics.service?.completePasswordSignUp(email.takeIf { method == "password" })
    Analytics.track(AnalyticsEvent.SIGN_IN_COMPLETED, "method" to method, "entry_point" to entry)
}
