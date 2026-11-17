package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosFilterPresentation
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosFilterViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import platform.UIKit.UIImage

/** Swift sees presentation and intent callbacks, never the search implementation. */
class IosDiscoveryFilters internal constructor(
    initial: DiscoveryIosFilterPresentation,
    private val media: IosCatalogueMedia,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(initial.state)
    private var presentation: DiscoveryIosFilterPresentation? = initial

    internal fun update(next: DiscoveryIosFilterPresentation) {
        if (presentation == null) return
        presentation = next
        state.value = next.state
    }

    internal fun dispose() {
        presentation = null
        scope.cancel()
    }

    fun currentState(): IosDiscoveryFilterState = state.value.toIos()

    fun observe(onState: (IosDiscoveryFilterState) -> Unit): IosDiscoveryFilterObservation =
        IosDiscoveryFilterObservation(scope.launch { state.collect { onState(it.toIos()) } })

    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosDiscoveryFilterObservation =
        IosDiscoveryFilterObservation(scope.launch { onImage(media.loadImage(url)) })

    fun reset() { presentation?.onReset?.invoke() }
    fun price(index: Int) { presentation?.onPrice?.invoke(index) }
    fun toggle(section: String, id: String) { presentation?.onToggle?.invoke(section, id) }
    fun retry() { presentation?.onRetry?.invoke() }
    fun close() = finish { it.onClose() }
    fun apply() = finish { it.onApply() }

    private fun finish(action: (DiscoveryIosFilterPresentation) -> Unit) {
        val active = presentation ?: return
        dispose()
        action(active)
    }
}

class IosDiscoveryFilterObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

data class IosDiscoveryFilterState(
    val title: String,
    val reset: String,
    val done: String,
    val close: String,
    val priceTitle: String,
    val prices: List<String>,
    val priceIndex: Int,
    val canReset: Boolean,
    val catalogStatus: String?,
    val isLoading: Boolean,
    val retry: String?,
    val sections: List<IosDiscoveryFilterSection>,
)

data class IosDiscoveryFilterSection(
    val id: String,
    val title: String,
    val selectedLabel: String?,
    val showAll: String,
    val collapse: String,
    val choices: List<IosDiscoveryFilterChoice>,
)

data class IosDiscoveryFilterChoice(val id: String, val title: String, val selected: Boolean, val photoUrl: String?)

private fun DiscoveryIosFilterViewState.toIos() = IosDiscoveryFilterState(
    title, reset, done, close, priceTitle, prices, priceIndex, canReset, catalogStatus, isLoading, retry,
    sections.map { section ->
        IosDiscoveryFilterSection(section.id, section.title, section.selectedLabel, section.showAll, section.collapse,
            section.choices.map { IosDiscoveryFilterChoice(it.id, it.title, it.selected, it.photoUrl) })
    },
)
