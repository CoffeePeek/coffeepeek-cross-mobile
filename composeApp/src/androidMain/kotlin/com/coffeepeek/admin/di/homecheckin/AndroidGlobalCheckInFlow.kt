package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.admin.di.checkinhistory.HistoryCreationAnalytics
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** App navigation/lease only; the feature owns form state, validation, photos and writes. */
internal class AndroidGlobalCheckInFlow(
    sessions: SessionReader,
    private val createEditor: (AndroidCheckInHistorySession, CheckInTracker?) -> HomeCheckInEditorEntry,
) : ViewModel() {
    val lease = AndroidCheckInHistorySession(sessions)
    private val analytics = HistoryCreationAnalytics(trackAttempt = false) { canAccess() }
    private val mutableState = MutableStateFlow<GlobalCheckInDestination>(GlobalCheckInDestination.Picker)
    val state = mutableState.asStateFlow()
    private var tracker: CheckInTracker? = null
    private var delivered = false

    init {
        if (canAccess()) analytics.opened() else revoke()
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().catch { error ->
                if (error is CancellationException) throw error
                revoke()
            }.collect { if (!lease.isCurrent(it)) revoke() }
        }
    }

    fun canAccess() = lease.isLoggedIn && lease.isCurrent()

    fun home() {
        if (!canAccess() || mutableState.value !== GlobalCheckInDestination.Picker) return
        tracker = analytics.home()
        mutableState.value = GlobalCheckInDestination.Home(createEditor(lease, tracker))
    }

    fun shop() {
        if (!canAccess() || mutableState.value !== GlobalCheckInDestination.Picker) return
        tracker = analytics.shop()
        mutableState.value = GlobalCheckInDestination.Closed(GlobalCheckInOutcome.Shop)
    }

    /** Adding a bag is not a check-in: the picker closes and the application opens the scan. */
    fun shelfBag() {
        if (!canAccess() || mutableState.value !== GlobalCheckInDestination.Picker) return
        tracker = null
        mutableState.value = GlobalCheckInDestination.Closed(GlobalCheckInOutcome.ShelfBag)
    }

    fun cancel(expected: GlobalCheckInDestination) {
        if (!owns(expected) || expected is GlobalCheckInDestination.Closed ||
            expected is GlobalCheckInDestination.Home && !expected.dismissAllowed.value) return
        cancelTracker()
        mutableState.value = GlobalCheckInDestination.Closed(GlobalCheckInOutcome.Close)
    }

    fun setDismissAllowed(expected: GlobalCheckInDestination.Home, allowed: Boolean) {
        if (owns(expected)) expected.setDismissAllowed(allowed)
    }

    fun saved(expected: GlobalCheckInDestination.Home, id: String) {
        if (!owns(expected) || id.isBlank()) return
        // Accepted save is terminal even while the outer sheet still reports busy.
        tracker = null
        mutableState.value = GlobalCheckInDestination.Closed(GlobalCheckInOutcome.Saved)
    }

    /** Terminal callback is consumed once while STARTED, with the latest caller and lease. */
    fun claim(expected: GlobalCheckInDestination.Closed): GlobalCheckInCompletion? {
        if (delivered || mutableState.value !== expected) return null
        delivered = true
        val outcome = if (canAccess()) expected.outcome else GlobalCheckInOutcome.Close
        return GlobalCheckInCompletion(outcome, tracker.takeIf { outcome == GlobalCheckInOutcome.Shop })
            .also { tracker = null }
    }

    private fun owns(expected: GlobalCheckInDestination) = canAccess() && mutableState.value === expected

    private fun cancelTracker() {
        if (canAccess()) {
            analytics.cancelled()
            tracker?.cancel("close")
        }
        tracker = null
    }

    private fun revoke() {
        lease.revoke()
        tracker = null
        mutableState.value = GlobalCheckInDestination.Closed(GlobalCheckInOutcome.Close)
    }

    override fun onCleared() {
        if (!delivered) cancelTracker()
        lease.revoke()
    }
}

internal sealed interface GlobalCheckInDestination {
    data object Picker : GlobalCheckInDestination
    class Home(val entry: HomeCheckInEditorEntry) : GlobalCheckInDestination {
        private val mutableDismissAllowed = MutableStateFlow(true)
        val dismissAllowed = mutableDismissAllowed.asStateFlow()
        fun setDismissAllowed(allowed: Boolean) { mutableDismissAllowed.value = allowed }
    }
    class Closed(val outcome: GlobalCheckInOutcome) : GlobalCheckInDestination
}

internal enum class GlobalCheckInOutcome { Close, Saved, Shop, ShelfBag }
internal class GlobalCheckInCompletion(val outcome: GlobalCheckInOutcome, val shopTracker: CheckInTracker?)
