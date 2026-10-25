package com.coffeepeek.admin.di.contentreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal data class ContentReportHostState(val generation: Int, val session: ContentReportSession, val unavailable: Boolean = false)

internal class ContentReportHostViewModel(private val sessions: SessionReader) : ViewModel() {
    private val mutableState = MutableStateFlow(ContentReportHostState(0, ContentReportSession(sessions)))
    val state = mutableState.asStateFlow()
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
                replace(null, unavailable = true)
            }.collect { value -> if (!mutableState.value.session.isCurrent(value)) replace(value) }
        }
    }

    private fun replace(value: Session?, unavailable: Boolean = false) {
        val old = mutableState.value
        old.session.revoke()
        mutableState.value = ContentReportHostState(old.generation + 1, ContentReportSession(sessions, value), unavailable)
    }

    override fun onCleared() { mutableState.value.session.revoke() }
}
