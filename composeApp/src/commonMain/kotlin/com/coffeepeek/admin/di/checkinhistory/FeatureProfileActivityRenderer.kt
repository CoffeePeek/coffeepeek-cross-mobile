package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.admin.di.media.CheckInPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.checkinhistory.api.ProfileHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.repository.*
import com.coffeepeek.feature.checkinhistory.impl.createProfileHistoryEntry
import com.coffeepeek.feature.profile.api.OwnProfileHistory

/** One revocable account lease supplies both calendar and history to the profile layout. */
internal class FeatureProfileActivityRenderer(
    private val sessions: SessionReader, private val shops: CheckInHistoryRepository,
    private val homes: HomeHistoryReader, private val calendar: AppCheckInHistoryCalendar,
) {
    @Composable fun History(content: @Composable (OwnProfileHistory) -> Unit) {
        val dateLabel by rememberUpdatedState(rememberCheckInDateLabel())
        val host = viewModel(key = "profile-history-host") { CheckInHistoryHostViewModel(sessions) }
        val session by host.session.collectAsState()
        if (session.observationFailed) HistorySessionUnavailable(host::retry, {})
        else if (session.lease.isCurrent() && session.lease.isLoggedIn) key(session.generation) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                val entry = remember(session) { createEntry(session) { dateLabel(it) } }
                HistoryContent(session, entry, content)
                val photo by session.photoPreview.collectAsState()
                photo?.let { CheckInPhotoViewer(CoffeePeekPhotoViewerImages(it.urls, it.originalUrls),
                    initialIndex = it.initialIndex, onDismiss = session::dismissPhoto) }
            }
        }
    }

    private fun createEntry(session: HistoryHostSession, formatDate: (String) -> String): ProfileHistoryEntry =
        createProfileHistoryEntry(session.lease.bind(shops), session.lease.bind(homes), calendar::snapshot,
            imageContent = { url, modifier, scale -> CoffeePeekImage(data = url, modifier = modifier, contentScale = scale) },
            formatDate = formatDate, modalContent = { AnalyticsOverlay(it) },
            highQualityImage = { url, highQualityUrl, modifier, scale ->
                CoffeePeekImage(data = url, highQualityData = highQualityUrl, modifier = modifier, contentScale = scale)
            })

    @Composable private fun HistoryContent(session: HistoryHostSession, entry: ProfileHistoryEntry,
        content: @Composable (OwnProfileHistory) -> Unit) {
        val generation by session.contentGeneration.collectAsState()
        val requested by Navigator.pendingProfileCheckIns.collectAsState()
        entry.Content(sessionGeneration = session.generation, contentGeneration = generation,
            showAllRequested = requested, onShowAllConsumed = Navigator::consumeProfileCheckIns,
            onOpenHome = { session.navigate(Navigator.Screen.HomeCheckInDetail(it)) },
            onOpenShop = { session.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenCheckIn = { session.navigate(Navigator.Screen.CheckInDetail(it)) },
            onOpenPhoto = session::openPhoto) { calendar, list, revision ->
            content(OwnProfileHistory(calendar, list, revision))
        }
    }

    private fun HistoryHostSession.navigate(screen: Navigator.Screen) {
        if (lease.isCurrent() && lease.isLoggedIn) Navigator.navigate(screen)
    }
}
