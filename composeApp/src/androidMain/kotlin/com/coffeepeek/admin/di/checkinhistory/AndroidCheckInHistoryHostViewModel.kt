package com.coffeepeek.admin.di.checkinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Account observation outlives visible composition, including when another route covers history. */
internal class AndroidCheckInHistoryHostViewModel(private val sessions: SessionRepository) : ViewModel() {
    private val mutableSession = MutableStateFlow(HistoryHostSession(0, AndroidCheckInHistorySession(sessions)))
    val session = mutableSession.asStateFlow()
    private var observation: Job? = null

    init { observe() }

    fun retry() {
        observation?.cancel()
        replace(sessions.peekSession())
        observe()
    }

    private fun observe() {
        observation = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().catch { error ->
                if (error is CancellationException) throw error
                replace(null, observationFailed = true)
            }.collect { value ->
                if (!mutableSession.value.lease.isCurrent(value)) replace(value)
            }
        }
    }

    private fun replace(value: Session?, observationFailed: Boolean = false) {
        val previous = mutableSession.value
        previous.clear()
        mutableSession.value = HistoryHostSession(previous.generation + 1,
            AndroidCheckInHistorySession(sessions, value), observationFailed)
    }

    override fun onCleared() {
        mutableSession.value.clear()
    }
}

internal class HistoryHostSession(
    val generation: Int,
    val lease: AndroidCheckInHistorySession,
    val observationFailed: Boolean = false,
) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private val mutablePhotoPreview = MutableStateFlow<HistoryPhotoPreview?>(null)
    val photoPreview = mutablePhotoPreview.asStateFlow()

    fun openPhoto(urls: List<String>, index: Int) {
        if (lease.isCurrent() && lease.isLoggedIn && urls.getOrNull(index)?.isNotBlank() == true) {
            mutablePhotoPreview.value = HistoryPhotoPreview(urls.toList(), index)
        }
    }

    fun dismissPhoto() { mutablePhotoPreview.value = null }

    fun clear() {
        lease.revoke()
        dismissPhoto()
        viewModelStore.clear()
    }
}
