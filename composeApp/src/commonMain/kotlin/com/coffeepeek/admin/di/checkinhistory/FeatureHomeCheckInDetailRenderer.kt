package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.checkins.HomeCheckInDetailRenderer
import com.coffeepeek.admin.di.media.CheckInPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.session.SessionReader

/** A Home detail/edit route, with the same revocable lease and editor lifetime as creation. */
internal class FeatureHomeCheckInDetailRenderer(
    private val sessions: SessionReader,
    private val homeEditors: HistoryHomeEditorFactory,
) : HomeCheckInDetailRenderer {
    @Composable
    override fun Content(id: String, editOnLoad: Boolean) {
        val host = viewModel(key = "home-check-in-detail-host") { CheckInHistoryHostViewModel(sessions) }
        val session by host.session.collectAsState()
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (session.observationFailed) HistorySessionUnavailable(host::retry, Navigator::popBack)
            else if (session.lease.isCurrent() && session.lease.isLoggedIn) key(session.generation) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                    Detail(id, editOnLoad, host, session)
                }
            } else LaunchedEffect(session) { Navigator.popBack() }
        }
    }

    @Composable
    private fun Detail(id: String, editOnLoad: Boolean, host: CheckInHistoryHostViewModel,
        session: HistoryHostSession) {
        val bridge = viewModel(key = "home-check-in-detail") { HistoryHomeBridge(session, homeEditors) }
        LaunchedEffect(bridge) {
            val initial = host.consumeInitialHomeId(id)
            if (initial != null) bridge.open(initial, editOnLoad)
            bridge.destination.collect { if (it == null) Navigator.popBack() }
        }
        HomeCheckInDestinationContent(bridge, session, session::openPhoto)
        val photo by session.photoPreview.collectAsState()
        photo?.let {
            CheckInPhotoViewer(CoffeePeekPhotoViewerImages(it.urls, it.originalUrls),
                it.initialIndex, session::dismissPhoto)
        }
    }
}
