package com.coffeepeek.admin.di.community

import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Platform-independent transport guard; the native presenter owns its lifetime. */
class IosCommunitySheet internal constructor(
    initial: String, busy: Boolean, callbacks: (String, String) -> Unit,
    private val accounts: SessionReader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val generation = accounts.accountGeneration()
    private val account = accounts.peekSession()?.userId
    private val signedIn = accounts.isActiveSession(accounts.peekSession())
    private val state = MutableStateFlow(initial)
    private var actions: ((String, String) -> Unit)? = callbacks
    private var sending = busy
    private var closed = false

    init {
        scope.launch {
            accounts.observeSession().catch {
                if (it is CancellationException) throw it
                revoke()
            }.collect { if (!isCurrent()) revoke() }
        }
    }

    fun current(): String = if (isCurrent()) state.value else "{\"active\":false}"
    fun isBusy() = isCurrent() && sending
    fun observe(onState: (String) -> Unit) = IosCommunitySheetObservation(scope.launch { state.collect { onState(current()) } })

    fun perform(action: String, value: String) {
        if (!isCurrent() || sending) return
        actions?.invoke(action, value)
    }

    internal fun update(json: String, busy: Boolean, callbacks: (String, String) -> Unit) {
        if (!isCurrent()) return
        sending = busy
        actions = callbacks
        state.value = json
    }

    internal fun close() {
        revoke()
        scope.cancel()
    }

    private fun revoke() {
        closed = true
        actions = null
        state.value = "{\"active\":false}"
    }

    private fun isCurrent() = !closed && signedIn && accounts.accountGeneration() == generation &&
        accounts.peekSession()?.userId == account && accounts.isActiveSession(accounts.peekSession())
}

class IosCommunitySheetObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
