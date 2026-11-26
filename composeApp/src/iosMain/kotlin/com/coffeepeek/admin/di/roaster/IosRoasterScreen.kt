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
    private val dispose: () -> Unit = {},
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val generation = accounts.accountGeneration()
    private val account = accounts.peekSession()?.userId
    private val signedIn = accounts.isActiveSession(accounts.peekSession())
    private val revoked = MutableStateFlow(false)
    private var closed = false
    private var released = false
    internal val fractions = MutableStateFlow<Map<String, Double>>(emptyMap())
    internal val screenVisible = MutableStateFlow(false)
    fun setVisible(visible: Boolean) { screenVisible.value = visible && isCurrent() }
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
        val accountChanges = accounts.observeSession().onEach { if (!isCurrent()) revoke() }.catch {
            if (it is CancellationException) throw it
            revoke()
            emit(null)
        }
        combine(flow, accountChanges) { _, _ -> Unit }.collect { receive() }
    })
    protected fun guarded(action: () -> Unit) { if (isCurrent()) action() }
    internal fun isCurrent(): Boolean {
        return !revoked.value && accounts.accountGeneration() == generation && accounts.peekSession()?.userId == account &&
            accounts.isActiveSession(accounts.peekSession()) == signedIn
    }
    fun close() {
        if (closed) return
        closed = true
        revoke()
        scope.cancel()
    }
    private fun revoke() {
        revoked.value = true
        screenVisible.value = false
        fractions.value = emptyMap()
        if (!released) { released = true; dispose() }
    }
}

class IosRoasterObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
