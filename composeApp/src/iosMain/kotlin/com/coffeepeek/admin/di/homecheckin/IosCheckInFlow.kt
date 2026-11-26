package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.analytics.NativeScreenAnalytics
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.shop.CheckInFlowHandoff
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Root presentation switches native controllers; the existing flow owns its one creation intent. */
class IosCheckInFlow internal constructor(renderer: FeatureCheckInCreateFlowRenderer, bean: HomeCheckInBean?,
    private val language: LanguagePort, private val media: IosCatalogueMedia, private val accounts: SessionReader,
    private val onSaved: () -> Unit, private val onFinished: () -> Unit,
) {
    private val flow = renderer.nativeFlow(bean)
    private val store = ViewModelStore().apply { put("native-create-flow", flow) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val visible = MutableStateFlow(false)
    private val output = MutableStateFlow(IosCheckInFlowState())
    private var closed = false
    private var completed: GlobalCheckInDestination.Closed? = null
    private var editor: IosHomeCheckInEditor? = null

    init {
        NativeScreenAnalytics(scope, null, visible).feature("home_check_in", "check_in_type_picker", visible)
        scope.launch { flow.state.collectLatest { destination ->
            when (destination) {
                GlobalCheckInDestination.Picker -> output.value = IosCheckInFlowState(picker = picker())
                is GlobalCheckInDestination.Home -> home(destination)
                is GlobalCheckInDestination.Closed -> {
                    completed = destination
                    output.value = IosCheckInFlowState(finished = true)
                }
            }
        } }
    }
    fun current() = output.value
    fun observe(receive: (IosCheckInFlowState) -> Unit) = IosHomeCheckInObservation(scope.launch { output.collect(receive) })
    fun requestClose() {
        if (closed) return
        editor?.requestClose() ?: flow.cancel(GlobalCheckInDestination.Picker)
    }
    /** Called after UIKit confirms dismissal, so the next route never races the old modal. */
    fun finishDismissal() {
        if (closed) return
        val completion = completed?.let(flow::claim) ?: return
        completed = null
        when (completion.outcome) {
            GlobalCheckInOutcome.Shop -> {
                CheckInFlowHandoff.tracker = completion.shopTracker
                Navigator.navigate(Navigator.Screen.CreateCheckIn)
            }
            GlobalCheckInOutcome.ShelfBag -> Navigator.navigate(Navigator.Screen.ShelfAddBag)
            GlobalCheckInOutcome.Saved -> onSaved()
            GlobalCheckInOutcome.Close -> Unit
        }
        onFinished()
    }
    fun close() {
        if (closed) return
        closed = true
        completed = null
        editor?.revoke()
        editor = null
        output.value = IosCheckInFlowState(finished = true)
        scope.cancel()
        store.clear()
    }
    private fun picker() = IosCheckInCreatePicker(language.current().languageTag, flow::pickerActions) { visible.value = it && flow.canAccess() }

    private suspend fun home(destination: GlobalCheckInDestination.Home): Nothing = coroutineScope {
        val native = (destination.entry as NativeHomeCheckInEntry).createNative(null, 0, flow.homeBean)
        val source = ownedNativeHomeEditor(native, language, media, accounts)
        editor = source
        try {
            launch { native.state.collect { state ->
                flow.setDismissAllowed(destination, state.status.dismissAllowed)
                output.value = IosCheckInFlowState(editor = source, dismissAllowed = state.status.dismissAllowed)
            } }
            launch { native.events.collect { event ->
                if (flow.canAccess() && native.isCurrent()) when (event.kind) {
                    "close" -> flow.cancel(destination)
                    "saved" -> event.preparationId?.let { flow.saved(destination, it) }
                }
            } }
            awaitCancellation()
        } finally {
            source.revoke()
            if (editor === source) editor = null
            coroutineContext.cancelChildren()
        }
    }
}

data class IosCheckInFlowState(val picker: IosCheckInCreatePicker? = null, val editor: IosHomeCheckInEditor? = null,
    val dismissAllowed: Boolean = true, val finished: Boolean = false)
