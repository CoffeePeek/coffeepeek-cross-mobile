package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.impl.createCheckInHistoryEntry

/** Android-only route adapter; account-owned screen and photo state share one revocable lifetime. */
internal class AndroidCheckInHistoryScreenRenderer(
    private val sessions: SessionRepository,
    private val repository: CheckInHistoryRepository,
    private val calendar: AndroidCheckInHistoryCalendar,
    private val imageContent: @Composable (String, Modifier, ContentScale) -> Unit = { url, modifier, scale ->
        CpImage(data = url, modifier = modifier, contentScale = scale)
    },
    private val photoContent: @Composable (List<String>, Int, () -> Unit) -> Unit = { urls, index, dismiss ->
        FullScreenImageDialog(imageUrls = urls, initialIndex = index, onDismiss = dismiss)
    },
) : CheckInHistoryScreenRenderer {
    @Composable
    override fun Content() {
        Content(formatCheckInDate = rememberCheckInDateLabel(), onBack = Navigator::popBack,
            onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) })
    }

    @Composable
    fun Content(
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit,
    ) {
        val host = viewModel(key = "android-check-in-history-host") { AndroidCheckInHistoryHostViewModel(sessions) }
        val session by host.session.collectAsStateWithLifecycle()
        if (session.observationFailed) {
            HistorySessionUnavailable(host::retry, onBack)
        } else if (session.lease.isCurrent()) {
            key(session.generation) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                    val photoPreview by session.photoPreview.collectAsStateWithLifecycle()
                    val entry = remember(session) {
                        createCheckInHistoryEntry(session.lease.bind(repository), calendar::snapshot, imageContent)
                    }
                    entry.Content(sessionGeneration = session.generation,
                        isLoggedIn = session.lease.isLoggedIn, formatCheckInDate = formatCheckInDate,
                        onBack = { if (session.lease.isCurrent()) onBack() },
                        onSignIn = { if (session.lease.isCurrent()) onSignIn() },
                        onOpenShop = { if (session.lease.isCurrent()) onOpenShop(it) },
                        onOpenPhoto = session::openPhoto)
                    photoPreview?.let { photoContent(it.urls, it.initialIndex, session::dismissPhoto) }
                }
            }
        }
    }
}
