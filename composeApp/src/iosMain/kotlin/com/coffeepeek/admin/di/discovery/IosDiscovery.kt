@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.roaster.IosRoasterDiscovery
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosPresentation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.UIKit.UIImage
import platform.UIKit.UIView

/** One native surface; account generations revoke actions, protected media and child owner slots together. */
class IosDiscovery internal constructor(
    initial: DiscoveryIosPresentation, private val media: IosCatalogueMedia,
    private val accounts: SessionReader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val account = accounts.discoveryAccount()
    private var closed = false
    private val presentation = MutableStateFlow<DiscoveryIosPresentation?>(initial)
    private val state = MutableStateFlow(initial.state)
    private val roasters = MutableStateFlow<IosRoasterDiscovery?>(null)
    internal val visibleElements = MutableStateFlow<Set<String>>(emptySet())
    private val map = IosDiscoveryMap(scope, { presentation.value }, ::isCurrent)

    init { scope.launch { accounts.observeSession().collect { if (!isCurrent()) revoke() } } }

    fun currentState() = state.value.toNative()
    fun observe(onState: (IosDiscoveryState) -> Unit) =
        IosDiscoveryObservation(scope.launch { state.collect { onState(it.toNative()) } })

    internal fun update(data: DiscoveryIosPresentation) {
        if (!isCurrent()) return
        presentation.value = data
        state.value = data.state
        map.update()
    }

    fun perform(name: String, id: String) {
        if (!isInteractive()) return
        if (name in setOf("suggestion", "map-select", "map-open", "carousel")) map.dismissInput()
        presentation.value?.onAction?.invoke(name, id)
    }

    fun query(text: String) { if (isInteractive()) presentation.value?.onQuery?.invoke(text) }
    fun visible(name: String, shown: Boolean) {
        visibleElements.value = if (shown && !closed) visibleElements.value + name else visibleElements.value - name
    }

    fun loadImage(url: String, onImage: (UIImage?) -> Unit) = IosDiscoveryObservation(scope.launch {
        try {
            accounts.observeSession().collectLatest {
                if (!isCurrent()) onImage(null)
                else media.observeImage(url, ::isCurrent).collect { onImage(it.takeIf { isCurrent() }) }
            }
        } catch (cancelled: CancellationException) {
            onImage(null)
            throw cancelled
        } catch (_: Exception) { onImage(null) }
    })

    fun createMapView(): UIView = map.createView()
    fun updateMapView(view: UIView, dark: Boolean) = map.attach(view, dark)

    fun currentRoasters(): IosRoasterDiscovery? = roasters.value.takeIf { isCurrent() }
    fun observeRoasters(onSource: (IosRoasterDiscovery?) -> Unit) =
        IosDiscoveryObservation(scope.launch { roasters.collect { onSource(it.takeIf { isCurrent() }) } })

    internal fun roastersChanged(source: IosRoasterDiscovery?) {
        roasters.value = source.takeIf { isCurrent() }
    }

    internal fun close() {
        revoke()
        scope.cancel()
    }

    private fun revoke() {
        closed = true
        presentation.value = null
        roasters.value = null
        state.value = state.value.copy(active = false, shops = emptyList(), recent = emptyList(), mapShops = emptyList())
        visibleElements.value = emptySet()
        map.close()
    }

    private fun isCurrent(): Boolean = !closed && accounts.discoveryAccount() == account
    private fun isInteractive(): Boolean = isCurrent() && state.value.active
}

class IosDiscoveryObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
