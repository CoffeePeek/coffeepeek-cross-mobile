package com.coffeepeek.admin.di.roaster

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.UIKit.UIImage

abstract class IosRoasterScreen internal constructor(
    private val language: LanguagePort,
    protected val accounts: SessionReader,
    private val images: (String, () -> Boolean) -> Flow<UIImage?>,
) {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val generation = accounts.accountGeneration()
    private val account = accounts.peekSession()?.userId
    private val signedIn = accounts.isActiveSession(accounts.peekSession())
    private var revoked = false
    internal val fractions = MutableStateFlow<Map<String, Double>>(emptyMap())
    fun languageTag() = language.current().languageTag
    fun observeLanguage(receive: (String) -> Unit) = IosRoasterObservation(scope.launch {
        language.observe().collect { receive(it.languageTag) }
    })
    fun visible(section: String, fraction: Double) {
        if (!isCurrent() || !fraction.isFinite()) return
        fractions.value = fractions.value.toMutableMap().apply {
            if (fraction <= 0) remove(section) else put(section, fraction.coerceIn(0.0, 1.0))
        }
    }
    fun loadImage(url: String, receive: (UIImage?) -> Unit) = IosRoasterObservation(scope.launch {
        images(url, ::isCurrent).collect(receive)
    })
    protected fun <T> observe(flow: Flow<T>, receive: () -> Unit) = IosRoasterObservation(scope.launch {
        val accountChanges = accounts.observeSession().catch {
            if (it is CancellationException) throw it
            revoked = true
            emit(null)
        }
        combine(flow, accountChanges) { _, _ -> Unit }.collect { receive() }
    })
    protected fun guarded(action: () -> Unit) { if (isCurrent()) action() }
    internal fun isCurrent(): Boolean {
        if (accounts.accountGeneration() != generation || accounts.peekSession()?.userId != account ||
            accounts.isActiveSession(accounts.peekSession()) != signedIn) revoked = true
        return !revoked
    }
    internal fun close() {
        revoked = true
        fractions.value = emptyMap()
        scope.cancel()
    }
}

class IosRoasterObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
