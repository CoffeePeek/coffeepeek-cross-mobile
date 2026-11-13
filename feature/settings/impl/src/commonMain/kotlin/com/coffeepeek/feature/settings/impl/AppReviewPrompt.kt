package com.coffeepeek.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.feature.settings.domain.AppReviewUsage
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/** Mount on a visible, idle screen where interrupting the user is appropriate. */
@Composable
fun AppReviewPrompt() {
    val platform = rememberAppReviewPlatform() ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val window = LocalWindowInfo.current
    if (!window.isWindowFocused) return
    LaunchedEffect(platform, lifecycle, window) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            platform.promptWhenEligible {
                lifecycle.currentState == Lifecycle.State.RESUMED && window.isWindowFocused
            }
        }
    }
}

internal class AppReviewPlatform(
    private val read: () -> String?,
    private val save: (String) -> Unit,
    private val canPresent: () -> Boolean,
    private val request: suspend (claim: () -> Boolean) -> Unit,
    private val now: () -> Long = { Clock.System.now().epochSeconds },
) {
    suspend fun promptWhenEligible(isActive: () -> Boolean) {
        if (!isActive() || !canPresent()) return
        val observedAt = now()
        val previous = read()?.toReviewUsage()
        val usage = previous?.recordUse(observedAt) ?: AppReviewUsage.firstUse(observedAt)
        if (usage != previous) save(usage.toStoredValue())
        if (!usage.canRequest(observedAt)) return
        delay(5.seconds)
        if (isActive() && canPresent()) request { isActive() && canPresent() && claimAttempt() }
    }

    // Store adapters claim on the main thread immediately before calling the store API.
    private fun claimAttempt(): Boolean {
        val usage = read()?.toReviewUsage() ?: return false
        val requestedAt = now()
        if (!usage.canRequest(requestedAt)) return false
        save(usage.copy(lastRequestedAt = requestedAt).toStoredValue())
        return true
    }
}

@Composable
internal expect fun rememberAppReviewPlatform(): AppReviewPlatform?

internal const val AppReviewUsageKey = "app_review_usage"

private fun AppReviewUsage.toStoredValue(): String =
    listOf(firstUsedAt, lastActiveDay, activeDays.toLong(), lastRequestedAt).joinToString(",")

private fun String.toReviewUsage(): AppReviewUsage? {
    val fields = split(',')
    if (fields.size != 4) return null
    val first = fields[0].toLongOrNull() ?: return null
    val lastDay = fields[1].toLongOrNull() ?: return null
    val count = fields[2].toIntOrNull() ?: return null
    val lastRequest = fields[3].toLongOrNull() ?: return null
    return AppReviewUsage(first, lastDay, count, lastRequest)
}
