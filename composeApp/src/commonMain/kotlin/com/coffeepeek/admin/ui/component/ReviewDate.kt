package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.ExperimentalTime

@Composable
internal fun rememberReviewDisplayDate(raw: String): String {
    val labels = reviewDateLabels()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(currentEpochMillis(), raw, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = currentEpochMillis()
                delay(60_000)
            }
        }
    }
    return remember(raw, now, labels) { formatReviewDisplayDate(raw, labels, now) }
}

internal data class ReviewDateLabels(
    val justNow: String,
    val ago: String,
    val minutes: List<String>,
    val hours: List<String>,
    val months: List<String>,
)

@Composable
internal fun reviewDateLabels() = ReviewDateLabels(
    stringResource(Res.string.review_just_now),
    stringResource(Res.string.review_ago),
    stringArrayResource(Res.array.minute_forms),
    stringArrayResource(Res.array.hour_forms),
    stringArrayResource(Res.array.review_months),
)

@OptIn(ExperimentalTime::class)
internal fun formatReviewDisplayDate(raw: String, labels: ReviewDateLabels, nowMillis: Long = currentEpochMillis()): String {
    val instant = parseServerInstant(raw.trim())
    val elapsed = instant?.let { nowMillis - it.toEpochMilliseconds() }
    if (elapsed != null && elapsed in 0 until 86_400_000L) {
        val minutes = elapsed / 60_000
        if (minutes == 0L) return labels.justNow
        val count = (if (minutes < 60) minutes else minutes / 60).toInt()
        val forms = if (minutes < 60) labels.minutes else labels.hours
        return "${pluralCountLabel(count, forms)} ${labels.ago}"
    }
    return reviewAbsoluteDate(raw, labels.months)
}

private fun reviewAbsoluteDate(raw: String, months: List<String>): String {
    val datePart = utcIsoToLocalDate(raw)
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val monthIndex = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return datePart
    val day = parts[2].trimStart('0').ifBlank { "0" }
    return "$day ${months[monthIndex - 1]} ${parts[0]}"
}
