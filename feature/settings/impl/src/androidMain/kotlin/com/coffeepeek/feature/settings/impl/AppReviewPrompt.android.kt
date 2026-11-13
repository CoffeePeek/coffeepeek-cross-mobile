package com.coffeepeek.feature.settings.impl

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

@Composable
internal actual fun rememberAppReviewPlatform(): AppReviewPlatform? {
    val activity = LocalActivity.current ?: return null
    return remember(activity) {
        val preferences = activity.getSharedPreferences(AppReviewUsageKey, Context.MODE_PRIVATE)
        val manager = ReviewManagerFactory.create(activity.applicationContext)
        AppReviewPlatform(
            read = { preferences.getString(AppReviewUsageKey, null) },
            save = { preferences.edit().putString(AppReviewUsageKey, it).apply() },
            canPresent = { !activity.isFinishing && !activity.isDestroyed && activity.hasWindowFocus() },
            request = { claim ->
                try {
                    val info = manager.requestReviewFlow().await()
                    if (claim()) manager.launchReviewFlow(activity, info)
                } catch (_: ReviewException) {
                    // Play can be unavailable. Try again on a later eligible visit.
                }
            },
        )
    }
}
