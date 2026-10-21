package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.createCheckInHistoryEntry

/** Android history, Home detail/editor entries and photo viewer share a revocable account. */
internal class AndroidCheckInHistoryScreenRenderer(
    private val sessions: SessionRepository,
    private val repository: CheckInHistoryRepository,
    private val homeReader: HomeHistoryReader,
    private val calendar: AndroidCheckInHistoryCalendar,
    private val homeEditors: HistoryHomeEditorFactory,
    private val imageContent: @Composable (String, Modifier, ContentScale) -> Unit = { url, modifier, scale ->
        CpImage(data = url, modifier = modifier, contentScale = scale)
    },
    private val photoContent: @Composable (List<String>, List<String>, Int, () -> Unit) -> Unit = { urls, originals, index, dismiss ->
        FullScreenImageDialog(images = PhotoViewerImages(urls, originals), initialIndex = index, onDismiss = dismiss)
    },
) : CheckInHistoryScreenRenderer {
    @Composable
    override fun Content(initialHomeId: String?) {
        RouteContent(initialHomeId, Navigator::popBack,
            { Navigator.navigate(Navigator.Screen.Auth) },
            { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            { Navigator.navigate(Navigator.Screen.CreateCheckIn) })
    }

    /** Actual route composition with injectable navigation for isolated tests. */
    @Composable
    fun RouteContent(initialHomeId: String? = null, onBack: () -> Unit, onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit, onCreateShop: () -> Unit) {
        val dateLabel = rememberCheckInDateLabel()
        Host(onBack) { host, session ->
            val creation = session.creationAnalytics
            val home = if (session.lease.isLoggedIn) viewModel(key = "history-home-bridge") {
                AndroidHistoryHomeBridge(session, homeEditors)
            } else null
            LaunchedEffect(host, initialHomeId, session, home) {
                // Consume only once the editor can really open: a stale lease or a guest must not drop the request.
                if (home != null && session.lease.isCurrent()) {
                    host.consumeInitialHomeId(initialHomeId)?.let { id -> home.open(id, editOnLoad = true) }
                }
            }
            val generation by session.contentGeneration.collectAsStateWithLifecycle()
            Entry(session, dateLabel, onBack, onSignIn, onOpenShop,
                { utcIsoToLocalDateTime(it).substringAfterLast(' ', "") },
                { home?.open(it) }, {
                    creation.shop()?.let { CheckInFlowHandoff.tracker = it }
                    onCreateShop()
                }, { home?.create(creation.home()) }, generation, creation)
            home?.let { HistoryHomeOverlay(it, session, session::openPhoto) }
        }
    }

    /** Callback seam for feature-entry contracts, without constructing a Home destination. */
    @Composable
    fun Content(formatCheckInDate: (String) -> String, onBack: () -> Unit, onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit, formatCheckInTime: (String) -> String, onOpenHome: (String) -> Unit,
        onCreateShop: () -> Unit, onCreateHome: () -> Unit, contentGeneration: Int = 0) {
        Host(onBack) { _, session ->
            Entry(session, formatCheckInDate, onBack, onSignIn, onOpenShop, formatCheckInTime,
                onOpenHome, onCreateShop, onCreateHome, contentGeneration)
        }
    }

    @Composable private fun Host(onBack: () -> Unit,
        content: @Composable (AndroidCheckInHistoryHostViewModel, HistoryHostSession) -> Unit) {
        val host = viewModel(key = "android-check-in-history-host") { AndroidCheckInHistoryHostViewModel(sessions) }
        val session by host.session.collectAsStateWithLifecycle()
        if (session.observationFailed) HistorySessionUnavailable(host::retry, onBack)
        else if (session.lease.isCurrent()) key(session.generation) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                content(host, session)
                val photo by session.photoPreview.collectAsStateWithLifecycle()
                photo?.let { photoContent(it.urls, it.originalUrls, it.initialIndex, session::dismissPhoto) }
            }
        }
    }

    @Composable private fun Entry(session: HistoryHostSession, formatDate: (String) -> String,
        onBack: () -> Unit, onSignIn: () -> Unit, onOpenShop: (String) -> Unit, formatTime: (String) -> String,
        onOpenHome: (String) -> Unit, onCreateShop: () -> Unit, onCreateHome: () -> Unit, contentGeneration: Int,
        creation: HistoryCreationAnalytics? = null) {
        val entry = remember(session) {
            createCheckInHistoryEntry(session.lease.bind(repository), session.lease.bind(homeReader), calendar::snapshot, imageContent,
                highQualityImage = { url, highQualityUrl, modifier, scale ->
                    CpImage(data = url, highQualityData = highQualityUrl, modifier = modifier, contentScale = scale)
                },
                createButtonModifier = {
                    val page = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
                    Modifier.analyticsExposure("check_in", "global_create", page)
                }, createPickerModifier = {
                    val page = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
                    Modifier.analyticsExposure("home_check_in", "check_in_type_picker", page)
                }, modalContent = { content -> AnalyticsOverlay(content) })
        }
        entry.Content(sessionGeneration = session.generation, isLoggedIn = session.lease.isLoggedIn,
            formatCheckInDate = formatDate, formatCheckInTime = formatTime, contentGeneration = contentGeneration,
            onBack = { if (session.lease.isCurrent()) onBack() },
            onSignIn = { if (session.lease.isCurrent()) onSignIn() },
            onOpenShop = { if (session.lease.isCurrent() && session.lease.isLoggedIn) onOpenShop(it) },
            onOpenPhoto = session::openPhoto,
            onOpenHome = { if (session.lease.isCurrent() && session.lease.isLoggedIn) onOpenHome(it) },
            onCreateShop = { if (session.lease.isCurrent() && session.lease.isLoggedIn) onCreateShop() },
            onCreateHome = { if (session.lease.isCurrent() && session.lease.isLoggedIn) onCreateHome() },
            onCreatePickerOpened = { if (session.lease.isCurrent()) creation?.opened() },
            onCreatePickerCancelled = { if (session.lease.isCurrent()) creation?.cancelled() })
    }
}
