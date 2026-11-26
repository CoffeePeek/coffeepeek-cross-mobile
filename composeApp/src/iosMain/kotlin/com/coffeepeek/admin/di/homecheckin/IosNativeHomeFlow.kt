@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.homecheckin

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.utils.IosNativePhotoPicker
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPickerController
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import platform.UIKit.UIViewController

/** One native presentation owns the flow, feature session, photo launcher and their cancellation. */
internal class IosNativeHomeFlow(renderer: FeatureCheckInCreateFlowRenderer, bean: HomeCheckInBean,
    language: LanguagePort, media: IosCatalogueMedia, accounts: SessionReader,
    private val onDismiss: () -> Unit, private val onSaved: () -> Unit) {
    private val flow = renderer.nativeFlow(bean)
    private val store = ViewModelStore().apply { put("native-home-flow", flow) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val home = flow.state.value as? GlobalCheckInDestination.Home
    private val session = home?.let { (it.entry as NativeHomeCheckInEntry).createNative(null, 0, bean) }
    private var editor: IosHomeCheckInEditor? = null
    private var closed = false
    private val overlay = correlationId("native-home")
    private var nativeController: UIViewController? = UIViewController()
    val controller: UIViewController get() = checkNotNull(nativeController)
    private val photos = IosNativePhotoPicker(scope) { nativeController }

    init {
        Analytics.service?.overlay(overlay, true)
        session?.let { native ->
            editor = IosHomeCheckInEditor(native, language, media, CheckInHistorySession(accounts), ::photoController)
            nativeController = homeEditorFactory(checkNotNull(editor))
            observeEditor(native)
        }
        scope.launch { flow.state.collect { if (it is GlobalCheckInDestination.Closed) complete(it) } }
    }

    private fun photoController() = HomeCheckInPhotoPickerController(photos.isPreparing,
        { maximum, receive -> photos.show(false, maximum) { receive(it.toHomePreparationPhotos(maximum)) } },
        { receive -> photos.show(true, 1) { receive(it.toHomePreparationPhotos(1)) } })

    private fun observeEditor(native: NativeHomeCheckInSession) {
        val destination = checkNotNull(home)
        scope.launch { native.state.collect { flow.setDismissAllowed(destination, it.status.dismissAllowed) } }
        scope.launch {
            native.events.collect {
                if (flow.canAccess() && native.isCurrent()) when (it.kind) {
                    "close" -> flow.cancel(destination)
                    "saved" -> it.preparationId?.let { id -> flow.saved(destination, id) }
                }
            }
        }
    }

    private fun complete(destination: GlobalCheckInDestination.Closed) {
        val completion = flow.claim(destination) ?: return
        try { if (completion.outcome == GlobalCheckInOutcome.Saved) onSaved() }
        finally { onDismiss() }
    }

    fun close() {
        if (closed) return
        closed = true
        Analytics.service?.overlay(overlay, false)
        photos.close()
        scope.cancel()
        editor?.close()
        editor = null
        nativeController = null
        session?.close()
        store.clear()
    }
}
