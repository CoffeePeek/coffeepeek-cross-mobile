package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityCardActions
import com.coffeepeek.feature.community.api.CommunityCardPresentation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.UIKit.UIImage

/** One mounted card. Feature callbacks and protected-media permissions remain the sources of authority. */
class IosCommunityCard internal constructor(
    private var data: CommunityCardPresentation?,
    private var actions: CommunityCardActions?,
    private val media: IosCatalogueMedia,
    private val accounts: SessionReader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val generation = accounts.accountGeneration()
    private val account = accounts.peekSession()?.userId
    private val signedIn = accounts.isActiveSession(accounts.peekSession())
    private var closed = false
    private val state = MutableStateFlow(IosCommunityCardState(data, actions, true))
    internal val height = MutableStateFlow(240.0)

    init {
        scope.launch {
            accounts.observeSession().catch { error ->
                if (error is CancellationException) throw error
                revoke()
            }.collect { if (!isCurrent()) revoke() }
        }
    }

    fun current() = state.value
    fun observe(onState: (IosCommunityCardState) -> Unit) = IosCommunityCardObservation(scope.launch { state.collect(onState) })

    internal fun update(next: CommunityCardPresentation, callbacks: CommunityCardActions) {
        if (!isCurrent()) return
        data = next
        actions = callbacks
        state.value = IosCommunityCardState(data, actions, true)
    }

    fun perform(action: String) { if (isCurrent()) actions?.actions?.get(action)?.invoke() }
    fun openPhoto(index: Int) { if (isCurrent() && index >= 0 && index < (data?.body?.photos?.size ?: 0)) actions?.onPhoto?.invoke(index) }

    fun heightChanged(height: Double) {
        if (height.isFinite() && height > 0 && !closed) this.height.value = kotlin.math.ceil(height)
    }

    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosCommunityCardObservation {
        return IosCommunityCardObservation(scope.launch {
            try {
                accounts.observeSession().collectLatest {
                    if (!isCurrent()) onImage(null)
                    else media.observeImage(url, ::isCurrent).collect { onImage(it.takeIf { isCurrent() }) }
                }
            } catch (cancelled: CancellationException) {
                onImage(null)
                throw cancelled
            } catch (_: Exception) {
                revoke()
                onImage(null)
            }
        })
    }

    internal fun close() {
        revoke()
        scope.cancel()
    }

    private fun revoke() {
        closed = true
        data = null
        actions = null
        state.value = IosCommunityCardState(data, actions, false)
    }

    private fun isCurrent() = !closed && accounts.accountGeneration() == generation &&
        accounts.peekSession()?.userId == account && accounts.isActiveSession(accounts.peekSession()) == signedIn
}

class IosCommunityCardObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
