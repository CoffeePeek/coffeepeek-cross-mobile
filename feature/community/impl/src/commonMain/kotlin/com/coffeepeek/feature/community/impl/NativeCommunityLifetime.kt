package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityNativeHost
import com.coffeepeek.feature.community.impl.ui.NativeCommunityResources
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal data class NativeCommunityRuntime(val started: Boolean = false, val bottomClearance: Double = 0.0)

/** One owned account/resource/ticker lifetime for either native community entry. */
internal class NativeCommunityLifetime(private val account: CommunityAccount, val host: CommunityNativeHost) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val runtime = MutableStateFlow(NativeCommunityRuntime())
    val resources = MutableStateFlow<NativeCommunityResources?>(null)
    val clock = MutableStateFlow(0L)
    private val accountKey = account.currentAccountKey()
    private var closed = false

    fun start(revoke: () -> Unit) {
        scope.launch { account.accountKey.catch {
            if (it is CancellationException) throw it
            revoke()
        }.collect { if (!isCurrent()) revoke() } }
        scope.launch { host.languages.distinctUntilChanged().collect { resources.value = NativeCommunityResources(it) } }
        scope.launch { runtime.map { it.started }.distinctUntilChanged().collectLatest { started ->
            if (started) while (isActive) {
                clock.value++
                delay(60_000)
            }
        } }
    }

    fun presentation(started: Boolean, bottomClearance: Double) {
        if (isCurrent()) runtime.value = NativeCommunityRuntime(started, bottomClearance.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0)
    }

    fun isCurrent() = !closed && account.currentAccountKey() == accountKey

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        resources.value = null
        runtime.value = NativeCommunityRuntime()
    }
}
