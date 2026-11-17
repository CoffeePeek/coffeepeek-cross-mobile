package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.checkinhistory.api.ProfileActivityEntry
import com.coffeepeek.feature.checkinhistory.api.ProfileActivityHeader
import com.coffeepeek.feature.checkinhistory.domain.repository.*
import com.coffeepeek.feature.checkinhistory.impl.createProfileActivityEntry

/** The same private read lease as history, owned independently by each profile surface. */
internal class FeatureProfileActivityRenderer(
    private val sessions: SessionReader, private val shops: CheckInHistoryRepository,
    private val homes: HomeHistoryReader, private val calendar: AppCheckInHistoryCalendar,
) {
    @Composable fun Calendar(selectedDate: String?, onSelectDate: (String?) -> Unit,
        expanded: Boolean, header: ProfileActivityHeader) {
        Host("profile-activity-month") { session, entry ->
            entry.Calendar(true, selectedDate, { if (session.lease.isCurrent()) onSelectDate(it) }, expanded, header)
        }
    }

    @Composable private fun Host(ownerKey: String, content: @Composable (HistoryHostSession, ProfileActivityEntry) -> Unit) {
        val host = viewModel(key = ownerKey) { CheckInHistoryHostViewModel(sessions) }
        val session by host.session.collectAsState()
        if (session.observationFailed) HistorySessionUnavailable(host::retry, {})
        else if (session.lease.isCurrent() && session.lease.isLoggedIn) key(session.generation) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                val entry = remember(session) {
                    createProfileActivityEntry(session.lease.bind(shops), session.lease.bind(homes), calendar::snapshot,
                        { url, modifier, scale -> CoffeePeekImage(data = url, modifier = modifier, contentScale = scale) })
                }
                content(session, entry)
            }
        }
    }
}
