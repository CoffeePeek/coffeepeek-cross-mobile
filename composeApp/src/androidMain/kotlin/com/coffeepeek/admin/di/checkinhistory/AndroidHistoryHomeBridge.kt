package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.analytics.CheckInTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Account-owned lifetime/bookkeeping only. The legacy Home VM retains form and write rules. */
internal class AndroidHistoryHomeBridge(
    private val session: HistoryHostSession,
    private val factory: HistoryHomeEditorFactory,
) : ViewModel() {
    private val editorStore = ViewModelStore()
    private val mutableEditor = MutableStateFlow<HomeCheckInViewModel?>(null)
    val editor = mutableEditor.asStateFlow()
    private var revision = 0L
    private var lastRequest: Pair<String, Boolean>? = null

    fun open(id: String, editOnLoad: Boolean = false) {
        if (id.isBlank() || !canReplace()) return
        val editor = replaceEditor()
        lastRequest = id to editOnLoad
        editor.open(id, editOnLoad)
    }

    fun create(tracker: CheckInTracker? = null) {
        if (!canReplace()) return
        replaceEditor().create(tracker)
    }

    fun retry() { lastRequest?.let { open(it.first, it.second) } }

    /** Old sheet callbacks must not mutate a replacement editor, even for the same account. */
    fun withEditor(expected: HomeCheckInViewModel, action: (HomeCheckInViewModel) -> Unit) {
        if (session.lease.isCurrent() && session.lease.isLoggedIn && mutableEditor.value === expected &&
            !expected.state.value.saving) action(expected)
    }

    fun dismiss() {
        val editor = mutableEditor.value ?: return
        withEditor(editor) {
            val wasEditing = it.state.value.editing
            if (wasEditing) it.closeEditor()
            if (!wasEditing || it.state.value.selected == null) clearEditor()
        }
    }

    private fun canReplace() = session.lease.isCurrent() && session.lease.isLoggedIn &&
        mutableEditor.value?.state?.value?.saving != true

    private fun replaceEditor(): HomeCheckInViewModel {
        clearEditor()
        val currentRevision = revision
        val provider = ViewModelProvider.create(editorStore, viewModelFactory {
            initializer {
                factory.create(session.lease, session::invalidateContent) {
                    viewModelScope.launch {
                        if (revision == currentRevision && session.lease.isCurrent()) clearEditor()
                    }
                }
            }
        })
        return provider["home-editor", HomeCheckInViewModel::class].also { mutableEditor.value = it }
    }

    private fun clearEditor() {
        revision++
        mutableEditor.value = null
        lastRequest = null
        editorStore.clear()
    }

    override fun onCleared() { clearEditor() }
}
