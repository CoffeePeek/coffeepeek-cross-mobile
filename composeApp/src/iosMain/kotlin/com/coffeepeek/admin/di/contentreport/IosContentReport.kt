package com.coffeepeek.admin.di.contentreport

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import com.coffeepeek.feature.contentreport.impl.NativeContentReport
import com.coffeepeek.feature.contentreport.impl.NativeContentReportEvent
import com.coffeepeek.feature.contentreport.impl.NativeContentReportInput
import com.coffeepeek.feature.contentreport.impl.NativeContentReportState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class IosContentReportState(
    val text: String = "",
    val preview: Boolean = false,
    val accessAvailable: Boolean = true,
    val canEdit: Boolean = true,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val deliveryUnconfirmed: Boolean = false,
    val errorKey: String? = null,
    val serverMessage: String? = null,
) {
    val canSubmit get() = canEdit && text.isNotBlank()
}

/** Reuses account renewal, replacement and revocation from the Compose route's host. */
class IosContentReport internal constructor(
    id: String,
    isCheckIn: Boolean,
    preview: Boolean,
    sessions: SessionReader,
    repository: ContentReportRepository,
    private val onBack: () -> Unit,
    private val onSignIn: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private val state = MutableStateFlow(IosContentReportState(preview = preview))
    private var active: ActiveReport? = null
    private var closed = false

    init {
        if (preview) {
            val input = NativeContentReportInput(id, isCheckIn, preview = true, loggedIn = false, accessAvailable = true)
            val report = NativeContentReport(input, repository) { true }
            scope.launch { present(report, owns = { true }, onEvent = ::handlePreview) }
        } else {
            val host = ContentReportHostViewModel(sessions).also { store.put("account", it) }
            scope.launch { host.state.collectLatest { owner -> presentAccount(host, owner, id, isCheckIn, repository) } }
        }
    }

    fun currentState(): IosContentReportState = state.value

    fun observe(onState: (IosContentReportState) -> Unit): IosContentReportObservation =
        IosContentReportObservation(scope.launch { state.collect(onState) })

    fun changeText(text: String) { currentReport()?.changeText(text) }
    fun submit() { currentReport()?.submit() }
    fun back() { currentReport()?.back() }
    fun retryAccess() { currentReport()?.retryAccess() }
    fun resume() { currentReport()?.resume() }

    fun close() {
        if (closed) return
        closed = true
        store.clear()
        scope.cancel()
        active = null
        state.value = IosContentReportState(preview = state.value.preview, canEdit = false)
    }

    private fun currentReport(): NativeContentReport? = active?.takeIf { !closed && it.owns() }?.report

    private fun handlePreview(event: NativeContentReportEvent) {
        if (event == NativeContentReportEvent.Back) onBack()
    }

    private suspend fun presentAccount(
        host: ContentReportHostViewModel, owner: ContentReportHostState, id: String, isCheckIn: Boolean,
        repository: ContentReportRepository,
    ) {
        val input = NativeContentReportInput(id, isCheckIn, false, owner.session.isLoggedIn, !owner.unavailable)
        val report = NativeContentReport(input, owner.session.bind(repository), owner.session::isCurrent)
        present(report, owns = { host.state.value === owner }) { event ->
            when (event) {
                NativeContentReportEvent.Back -> onBack()
                NativeContentReportEvent.SignIn -> if (owner.session.isCurrent()) onSignIn()
                NativeContentReportEvent.RetryAccess -> host.retry()
            }
        }
    }

    private suspend fun present(
        report: NativeContentReport, owns: () -> Boolean, onEvent: (NativeContentReportEvent) -> Unit,
    ) = coroutineScope {
        val destination = ActiveReport(report, owns)
        active = destination
        state.value = report.currentState().toIos()
        launch { report.states.collect { if (!closed && owns()) state.value = it.toIos() } }
        try {
            report.events.collect { if (!closed && owns()) onEvent(it) }
        } finally {
            if (active === destination) active = null
            report.close()
        }
    }

    private data class ActiveReport(val report: NativeContentReport, val owns: () -> Boolean)
}

class IosContentReportObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

private fun NativeContentReportState.toIos() = IosContentReportState(
    text, preview, accessAvailable, canEdit, submitting, submitted, deliveryUnconfirmed, errorKey, serverMessage,
)
