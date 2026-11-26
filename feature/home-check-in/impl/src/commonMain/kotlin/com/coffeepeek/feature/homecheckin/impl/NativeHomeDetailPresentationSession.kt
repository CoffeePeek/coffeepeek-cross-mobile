package com.coffeepeek.feature.homecheckin.impl

import com.coffeepeek.feature.homecheckin.api.*
import com.coffeepeek.feature.homecheckin.impl.ui.NativeHomeDetailResources
import com.coffeepeek.feature.homecheckin.impl.ui.compose.nativeHomeDetailPresentation
import com.coffeepeek.feature.homecheckin.impl.ui.compose.nativeHomeDetailActions
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInDetailEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NativeHomeDetailNavigation(val close: () -> Unit, val edit: (String) -> Unit, val deleted: (String) -> Unit,
    val photo: (List<String>, List<String>, Int) -> Unit)
data class NativeHomeDetailSnapshot(val data: HomeCheckInDetailPresentation, val actions: HomeCheckInDetailCallbacks)

/** Plain resource/event adapter shared with the existing detail state machine. */
class NativeHomeDetailPresentationSession(private val core: NativeHomeDetailSession, languages: Flow<String>,
    private val formatDate: (String, String) -> String, private val formatMethod: (String) -> String,
    private val notice: () -> String?, private val navigation: NativeHomeDetailNavigation,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resources = MutableStateFlow<NativeHomeDetailResources?>(null)
    private val output = MutableStateFlow<NativeHomeDetailSnapshot?>(null)
    val state = output.asStateFlow()
    private val visible = MutableStateFlow(false)
    init {
        scope.launch { languages.collectLatest { resources.value = NativeHomeDetailResources() } }
        observeState()
        observeEvents()
    }
    fun visibility(shown: Boolean) { visible.value = shown && core.isCurrent() }
    private fun observeState() {
        scope.launch { combine(resources.filterNotNull(), core.state) { resources, state -> resources to state }
            .collectLatest { (resources, state) ->
                if (!core.isCurrent()) return@collectLatest
                val data = resources.nativeHomeDetailPresentation(state, formatDate, formatMethod, notice())
                if (core.isCurrent()) output.value = NativeHomeDetailSnapshot(data, nativeHomeDetailActions(state, core::action))
            } }
    }
    private fun observeEvents() {
        scope.launch { visible.collectLatest { shown ->
            if (shown) core.events.collect { envelope ->
                if (!core.isCurrent()) return@collect
                when (val event = envelope.event) {
                    HomeCheckInDetailEvent.Close -> navigation.close()
                    is HomeCheckInDetailEvent.Edit -> navigation.edit(event.preparationId)
                    is HomeCheckInDetailEvent.Deleted -> navigation.deleted(event.preparationId)
                    is HomeCheckInDetailEvent.OpenPhoto -> navigation.photo(event.urls, event.originalUrls, event.index)
                }
            }
        } }
    }
    fun close() {
        core.close()
        output.value = null
        scope.cancel()
    }
}
