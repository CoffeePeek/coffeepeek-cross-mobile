package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.coffeepeek.admin.analytics.CheckInTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Navigation/lifetime only. Feature entries own loading, drafts, validation and writes. */
internal class HistoryHomeBridge(
    private val session: HistoryHostSession,
    private val factory: HistoryHomeEditorFactory,
) : ViewModel() {
    private val mutableDestination = MutableStateFlow<HistoryHomeDestination?>(null)
    val destination = mutableDestination.asStateFlow()

    fun open(id: String, editOnLoad: Boolean = false) {
        if (id.isNotBlank() && canReplace()) replace(id, editOnLoad)
    }

    fun create(tracker: CheckInTracker? = null) {
        if (canReplace()) replace(null, editing = true, tracker = tracker)
    }

    fun edit(expected: HistoryHomeDestination, id: String) {
        if (owns(expected) && expected.dismissAllowed.value && !expected.editing && expected.id == id) replace(id, true)
    }

    /** Capture the destination in every callback: a disposed sheet cannot affect its replacement. */
    fun dismiss(expected: HistoryHomeDestination) {
        if (!owns(expected) || !expected.dismissAllowed.value) return
        if (expected.editing && expected.id != null) replace(expected.id, editing = false)
        else clearDestination()
    }

    fun setDismissAllowed(expected: HistoryHomeDestination, allowed: Boolean) {
        if (owns(expected)) expected.setDismissAllowed(allowed)
    }

    fun saved(expected: HistoryHomeDestination, id: String, visibilityUnconfirmed: Boolean) {
        // A successful write may still have dismissal locked; it is not a user close request.
        if (!owns(expected) || !expected.editing || id.isBlank() || expected.id?.let { it != id } == true) return
        session.invalidateContent()
        replace(id, editing = false, visibilityUnconfirmed = visibilityUnconfirmed)
    }

    fun deleted(expected: HistoryHomeDestination, id: String) {
        if (!owns(expected) || expected.editing || expected.id != id) return
        session.invalidateContent()
        clearDestination()
    }

    fun openPhoto(expected: HistoryHomeDestination, urls: List<String>, index: Int, onPhoto: (List<String>, Int) -> Unit) {
        openPhoto(expected, urls, urls, index) { previews, _, selected -> onPhoto(previews, selected) }
    }

    fun openPhoto(expected: HistoryHomeDestination, urls: List<String>, originals: List<String>, index: Int,
        onPhoto: (List<String>, List<String>, Int) -> Unit) {
        if (owns(expected) && !expected.editing && expected.dismissAllowed.value &&
            urls.size == originals.size && index in originals.indices && originals.all(String::isNotBlank)) {
            onPhoto(urls.toList(), originals.toList(), index)
        }
    }

    private fun owns(expected: HistoryHomeDestination) = mutableDestination.value === expected &&
        session.lease.isCurrent() && session.lease.isLoggedIn

    private fun canReplace() = session.lease.isCurrent() && session.lease.isLoggedIn &&
        mutableDestination.value?.dismissAllowed?.value != false

    private fun replace(id: String?, editing: Boolean, visibilityUnconfirmed: Boolean = false, tracker: CheckInTracker? = null) {
        clearDestination()
        mutableDestination.value = HistoryHomeDestination(id, editing, visibilityUnconfirmed,
            factory.create(session.lease, tracker))
    }

    private fun clearDestination() {
        val previous = mutableDestination.value
        mutableDestination.value = null
        previous?.viewModelStore?.clear()
    }

    override fun onCleared() { clearDestination() }
}

/** Each detail has its own store, including a fresh read after editing or close/reopen. */
internal class HistoryHomeDestination(
    val id: String?,
    val editing: Boolean,
    val visibilityUnconfirmed: Boolean,
    val entries: HistoryHomeEntries,
) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private val mutableDismissAllowed = MutableStateFlow(true)
    val dismissAllowed = mutableDismissAllowed.asStateFlow()
    fun setDismissAllowed(allowed: Boolean) { mutableDismissAllowed.value = allowed }
}
