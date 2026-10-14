package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.impl.createCheckInHistoryEntry

/** Prepared application boundary; the active VisitedPlaces route is not switched by registering it. */
internal class AndroidCheckInHistoryScreenRenderer(
    private val sessions: SessionRepository,
    private val repository: CheckInHistoryRepository,
    private val calendar: AndroidCheckInHistoryCalendar,
) {
    @Composable
    fun Content(
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (String) -> Unit,
        onOpenPhoto: (List<String>, Int) -> Unit,
        contentGeneration: Int = 0,
    ) {
        val host = viewModel(key = "android-check-in-history-host") { AndroidCheckInHistoryHostViewModel(sessions) }
        val session by host.session.collectAsStateWithLifecycle()
        if (session.observationFailed) {
            HistorySessionUnavailable(host::retry, onBack)
        } else if (session.lease.isCurrent()) {
            key(session.generation) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                    val entry = remember(session) {
                        createCheckInHistoryEntry(session.lease.bind(repository), calendar::snapshot) { url, modifier, scale ->
                            CpImage(data = url, modifier = modifier, contentScale = scale)
                        }
                    }
                    entry.Content(sessionGeneration = session.generation,
                        isLoggedIn = session.lease.isLoggedIn, formatCheckInDate = formatCheckInDate,
                        onBack = { if (session.lease.isCurrent()) onBack() },
                        onSignIn = { if (session.lease.isCurrent()) onSignIn() },
                        onOpenShop = { if (session.lease.isCurrent()) onOpenShop(it) },
                        onOpenPhoto = { urls, index -> if (session.lease.isCurrent()) onOpenPhoto(urls, index) },
                        contentGeneration = contentGeneration)
                }
            }
        }
    }
}
