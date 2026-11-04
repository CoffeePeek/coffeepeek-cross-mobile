package com.coffeepeek.feature.checkinhistory.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.LocalHistoryShopTimelineContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.LocalHistoryHomeTimelineContent
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.ui.CombinedHistoryViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.CheckInHistoryScreen
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.LocalHistoryHighQualityImage
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryEvent
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarContext
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarProvider

/** Composition supplies an account-bound repository/store and its existing authenticated image loader. */
fun createCheckInHistoryEntry(
    repository: CheckInHistoryRepository,
    homeReader: HomeHistoryReader,
    calendarSnapshot: () -> CheckInHistoryCalendarSnapshot,
    imageContent: @Composable (url: String, modifier: Modifier, scale: ContentScale) -> Unit,
    createButtonModifier: @Composable () -> Modifier = { Modifier },
    createPickerModifier: @Composable () -> Modifier = { Modifier },
    modalContent: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
    /** Optional: card photos start with the thumbnail and upgrade to the detail variant (url, highQualityUrl, ...). */
    highQualityImage: (@Composable (url: String, highQualityUrl: String, modifier: Modifier, scale: ContentScale) -> Unit)? = null,
    shopTimelineContent: (@Composable (PersonalCheckIn, onOpen: () -> Unit) -> Unit)? = null,
    homeTimelineContent: (@Composable (HomeHistoryEntry, onOpen: () -> Unit) -> Unit)? = null,
): CheckInHistoryEntry = DefaultCheckInHistoryEntry(repository, homeReader, calendarSnapshot, imageContent,
    createButtonModifier, createPickerModifier, modalContent, highQualityImage, shopTimelineContent, homeTimelineContent)

private class DefaultCheckInHistoryEntry(
    private val repository: CheckInHistoryRepository,
    private val homeReader: HomeHistoryReader,
    private val calendarSnapshot: () -> CheckInHistoryCalendarSnapshot,
    private val imageContent: @Composable (String, Modifier, ContentScale) -> Unit,
    private val createButtonModifier: @Composable () -> Modifier,
    private val createPickerModifier: @Composable () -> Modifier,
    private val modalContent: @Composable (@Composable () -> Unit) -> Unit,
    private val highQualityImage: (@Composable (String, String, Modifier, ContentScale) -> Unit)?,
    private val shopTimelineContent: (@Composable (PersonalCheckIn, () -> Unit) -> Unit)?,
    private val homeTimelineContent: (@Composable (HomeHistoryEntry, () -> Unit) -> Unit)?,
) : CheckInHistoryEntry {
    @Composable
    override fun Content(
        sessionGeneration: Int,
        isLoggedIn: Boolean,
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit,
        onOpenPhoto: (List<String>, List<String>, Int) -> Unit,
        formatCheckInTime: (String) -> String,
        onOpenHome: (String) -> Unit,
        onCreateShop: () -> Unit,
        onCreateHome: () -> Unit,
        contentGeneration: Int,
        onCreatePickerOpened: () -> Unit,
        onCreatePickerCancelled: () -> Unit,
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
            CombinedHistoryViewModel(repository, homeReader, provider, isLoggedIn, sessionGeneration, contentGeneration)
        }
        CompositionLocalProvider(LocalHistoryHighQualityImage provides highQualityImage,
            LocalHistoryShopTimelineContent provides shopTimelineContent,
            LocalHistoryHomeTimelineContent provides homeTimelineContent) {
        CheckInHistoryScreen(viewModel, sessionGeneration, isLoggedIn, contentGeneration, formatCheckInDate, formatCheckInTime,
            imageContent, createButtonModifier(), createPickerModifier(), modalContent) { event ->
            when (event) {
                CheckInHistoryEvent.Back -> onBack()
                CheckInHistoryEvent.SignIn -> onSignIn()
                is CheckInHistoryEvent.OpenShop -> onOpenShop(event.slug)
                is CheckInHistoryEvent.OpenPhoto -> onOpenPhoto(event.urls, event.originalUrls, event.index)
                is CheckInHistoryEvent.OpenHome -> onOpenHome(event.id)
                is CheckInHistoryEvent.Create -> if (event.source == HistorySource.Shop) onCreateShop() else onCreateHome()
                CheckInHistoryEvent.CreatePickerOpened -> onCreatePickerOpened()
                CheckInHistoryEvent.CreatePickerCancelled -> onCreatePickerCancelled()
            }
        }
        }
    }
}
