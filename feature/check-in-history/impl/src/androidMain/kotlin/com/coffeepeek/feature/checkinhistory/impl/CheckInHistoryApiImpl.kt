package com.coffeepeek.feature.checkinhistory.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.impl.ui.CheckInHistoryViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.CheckInHistoryScreen
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryEvent
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarContext
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarProvider

/** Composition supplies an account-bound repository/store and its existing authenticated image loader. */
fun createCheckInHistoryEntry(
    repository: CheckInHistoryRepository,
    calendarSnapshot: () -> CheckInHistoryCalendarSnapshot,
    imageContent: @Composable (url: String, modifier: Modifier, scale: ContentScale) -> Unit,
): CheckInHistoryEntry = DefaultCheckInHistoryEntry(repository, calendarSnapshot, imageContent)

private class DefaultCheckInHistoryEntry(
    private val repository: CheckInHistoryRepository,
    private val calendarSnapshot: () -> CheckInHistoryCalendarSnapshot,
    private val imageContent: @Composable (String, Modifier, ContentScale) -> Unit,
) : CheckInHistoryEntry {
    @Composable
    override fun Content(
        sessionGeneration: Int,
        isLoggedIn: Boolean,
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit,
        onOpenPhoto: (List<String>, Int) -> Unit,
        contentGeneration: Int,
    ) {
        val provider = HistoryCalendarProvider {
            val snapshot = calendarSnapshot()
            object : HistoryCalendarContext {
                override val today = snapshot.today
                override val zoneId = snapshot.zoneId
                override fun period(month: HistoryMonth): HistoryPeriod = snapshot.monthPeriod(month.year, month.month)
                    .let { HistoryPeriod(it.fromInclusive, it.toExclusive) }
                override fun localDate(instant: String): String? = snapshot.localDate(instant)
            }
        }
        val viewModel = viewModel(key = "check-in-history") {
            CheckInHistoryViewModel(repository, provider, isLoggedIn, sessionGeneration, contentGeneration)
        }
        CheckInHistoryScreen(viewModel, sessionGeneration, isLoggedIn, contentGeneration, formatCheckInDate, imageContent) { event ->
            when (event) {
                CheckInHistoryEvent.Back -> onBack()
                CheckInHistoryEvent.SignIn -> onSignIn()
                is CheckInHistoryEvent.OpenShop -> onOpenShop(event.slug)
                is CheckInHistoryEvent.OpenPhoto -> onOpenPhoto(event.urls, event.index)
            }
        }
    }
}
