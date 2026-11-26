@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.roaster.IosRoasterDiscovery
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosPresentation
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosFilterPresentation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/** One native surface; account generations revoke actions, protected media and child owner slots together. */
class IosDiscovery internal constructor(
    initial: DiscoveryIosPresentation, private val media: IosCatalogueMedia,
    private val accounts: SessionReader,
    initiallyReady: Boolean = true,
    val minimumBottomClearance: Float = 0f,
    private val onVisibility: (Boolean) -> Unit = {},
    private val onClearance: (Float) -> Unit = {},
    onClose: () -> Unit = {},
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val account = accounts.discoveryAccount()
    private val revoked = MutableStateFlow(false)
    internal val failed = MutableStateFlow(false)
    private var disposed = false
    private var released = false
    private var onRelease = onClose
    internal val screenVisible = MutableStateFlow(false)
    private var presenter: () -> UIViewController? = { null }
    private val presentation = MutableStateFlow<DiscoveryIosPresentation?>(initial)
    private var state = initial.state
    private val display = MutableStateFlow(IosDiscoveryPresentation(initial.state.toNative(), null, initiallyReady))
    private val filters = MutableStateFlow<IosDiscoveryFilters?>(null)
    internal val visibleElements = MutableStateFlow<Set<String>>(emptySet())
    private val map = IosDiscoveryMap(scope, { presentation.value }, ::isCurrent)

    init { scope.launch { accounts.observeSession().catch {
        if (it is CancellationException) throw it
        fail()
    }.collect { if (!isCurrent()) revoke() } } }

    fun currentPresentation() = display.value
    internal fun fail() {
        failed.value = true
        revoke()
    }
    fun bindPresenter(presenter: () -> UIViewController?) { if (!revoked.value) this.presenter = presenter }
    internal fun presentationHost() = presenter().takeIf { isCurrent() }
    internal fun bindDisposal(dispose: () -> Unit) {
        if (released) dispose() else onRelease = dispose
    }
    fun setVisible(visible: Boolean) {
        val active = visible && isCurrent()
        screenVisible.value = active
        onVisibility(active)
    }
    fun bottomClearance(value: Float) { if (isCurrent() && value.isFinite()) onClearance(maxOf(value, minimumBottomClearance)) }
    fun observePresentation(onState: (IosDiscoveryPresentation) -> Unit) =
        IosDiscoveryObservation(scope.launch { display.collect(onState) })

    internal fun update(data: DiscoveryIosPresentation, ready: Boolean = true, roasters: IosRoasterDiscovery? = null) {
        if (!isCurrent()) return
        presentation.value = data
        state = data.state
        display.value = IosDiscoveryPresentation(state.toNative(), roasters, ready)
        map.update()
    }

    internal fun updateFilters(data: DiscoveryIosFilterPresentation?) {
        if (!isCurrent()) return
        if (data == null) {
            filters.value?.dispose()
            filters.value = null
        } else filters.value?.update(data) ?: run { filters.value = IosDiscoveryFilters(data, media) }
    }
    fun currentFilters() = filters.value.takeIf { isCurrent() }
    fun observeFilters(onFilters: (IosDiscoveryFilters?) -> Unit) = IosDiscoveryObservation(scope.launch {
        filters.collect { onFilters(it.takeIf { isCurrent() }) }
    })
    fun applyFilters() { if (isCurrent()) filters.value?.apply() }

    fun perform(name: String, id: String) {
        if (!isInteractive()) return
        if (name in setOf("suggestion", "map-select", "map-open", "carousel")) map.dismissInput()
        presentation.value?.onAction?.invoke(name, id)
    }

    fun query(text: String) { if (isInteractive()) presentation.value?.onQuery?.invoke(text) }
    fun visible(name: String, shown: Boolean) {
        visibleElements.value = if (shown && !revoked.value) visibleElements.value + name else visibleElements.value - name
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

    fun close() {
        if (disposed) return
        disposed = true
        revoke()
        scope.cancel()
    }

    private fun revoke() {
        revoked.value = true
        screenVisible.value = false
        presenter = { null }
        clearPresentation()
        release()
    }

    private fun clearPresentation() {
        filters.value?.dispose()
        filters.value = null
        presentation.value = null
        state = state.copy(active = false, shops = emptyList(), recent = emptyList(), mapShops = emptyList())
        display.value = IosDiscoveryPresentation(state.toNative(), null, false)
        visibleElements.value = emptySet()
        map.close()
    }

    private fun release() {
        if (released) return
        released = true
        val dispose = onRelease
        onRelease = {}
        dispose()
    }

    private fun isCurrent(): Boolean = !revoked.value && accounts.discoveryAccount() == account
    private fun isInteractive(): Boolean = isCurrent() && state.active
}

class IosDiscoveryObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
