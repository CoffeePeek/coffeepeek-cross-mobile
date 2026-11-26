package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** One observed account lifetime for the native owner and both shared models. */
internal class NativeDiscoveryAccount(private val source: DiscoveryAccount) : DiscoveryAccount {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val keys = MutableStateFlow(source.currentAccountKey())
    private val mutableFailed = MutableStateFlow(false)
    val failed = mutableFailed.asStateFlow()
    override val accountKey = keys.asStateFlow()
    private var closed = false

    init {
        scope.launch { source.accountKey.catch {
            if (it is CancellationException) throw it
            mutableFailed.value = true
            keys.value = null
        }.collect { keys.value = it } }
    }

    override fun currentAccountKey(): String? {
        if (closed || failed.value) return null
        return source.currentAccountKey()
    }
    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        keys.value = null
    }
}
