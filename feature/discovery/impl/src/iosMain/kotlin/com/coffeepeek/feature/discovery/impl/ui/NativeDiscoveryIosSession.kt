package com.coffeepeek.feature.discovery.impl.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Ordinary platform binding: resources are cancellable data preparation, not a UI host. */
class NativeDiscoveryIosSession(val session: NativeDiscoverySession, languages: Flow<String>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resources = MutableStateFlow<DiscoveryIosResources?>(null)
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val mutablePresentation = MutableStateFlow(presentation(session.current(), nativeDiscoveryLoadingState(session.picker)))
    val presentations = mutablePresentation.asStateFlow()
    private val mutableFilters = MutableStateFlow<DiscoveryIosFilterPresentation?>(null)
    val filters = mutableFilters.asStateFlow()
    private var closed = false

    init {
        scope.launch { languages.distinctUntilChanged().collect { resources.value = DiscoveryIosResources() } }
        observePresentation()
        observeFilters()
    }

    private fun observePresentation() {
        scope.launch { combine(resources.filterNotNull(), session.state) { resources, state -> resources to state }
            .collectLatest { (resources, state) ->
                mutablePresentation.value = presentation(state, resources.discoveryIosState(state, session.picker))
                mutableReady.value = true
            } }
    }
    private fun observeFilters() {
        scope.launch { combine(resources.filterNotNull(), session.filters) { resources, state -> resources to state }
            .collectLatest { (resources, state) ->
                mutableFilters.value = state?.let {
                    DiscoveryIosFilterPresentation(resources.discoveryIosFilterViewState(it.search, it.filters),
                        session::resetFilterDraft, session::filterPrice, session::filterToggle, session::applyFilters,
                        session::dismissFilters, session::retryFilterCatalogs)
                }
            } }
    }
    private fun presentation(snapshot: NativeDiscoverySnapshot, state: DiscoveryIosState) = DiscoveryIosPresentation(
        state, discoveryIosCanvas(snapshot.map, !snapshot.overlay.mapExpanded), session.canvasCallbacks(), snapshot.map.iosViewportCamera(),
        session::perform, session::query, { session.perform("settings", "") },
    )

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        session.close()
        mutableReady.value = false
        mutableFilters.value = null
        mutablePresentation.value = presentation(session.current(), nativeDiscoveryLoadingState(session.picker))
    }
}
