package com.coffeepeek.admin.analytics

internal fun signInAnalytics(userId: String?, method: String, entry: String, email: String? = null) {
    if (userId != null) Analytics.service?.signedIn(userId, method, entry, email)
}
