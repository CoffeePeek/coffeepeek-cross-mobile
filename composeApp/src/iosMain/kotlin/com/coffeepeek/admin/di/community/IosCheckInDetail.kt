package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityCardActions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import platform.UIKit.UIImage

/** Full-screen adapter shares the card's account lease, image permissions and callback guards. */
class IosCheckInDetail internal constructor(
    private var data: NativeCheckInDetailPresentation?, actions: CommunityCardActions,
    media: IosCatalogueMedia, accounts: SessionReader,
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val card = IosCommunityCard(data?.card, actions, media, accounts)
    private var disposed = false
    private var onClose: () -> Unit = {}
    private var onVisibility: (Boolean) -> Unit = {}
    var overlays: IosCommunityOverlays? = null
        private set
    internal val screenVisible = MutableStateFlow(false)
    internal fun bindOwner(overlays: IosCommunityOverlays?, visibility: (Boolean) -> Unit, close: () -> Unit) {
        this.overlays = overlays
        onVisibility = visibility
        onClose = close
        if (disposed) close()
    }
    fun setVisible(visible: Boolean) {
        screenVisible.value = visible && card.current().active
        onVisibility(screenVisible.value)
    }
    fun current() = IosCheckInDetailState(data, card.current())
    fun observe(onState: (IosCheckInDetailState) -> Unit) = IosCheckInDetailObservation(card.observe {
        if (!it.active) data = null
        onState(current())
    })
    fun perform(action: String) = card.perform(action)
    fun openPhoto(index: Int) = card.openPhoto(index)
    fun loadImage(url: String, onImage: (UIImage?) -> Unit) = IosCheckInDetailObservation(card.loadImage(url, onImage))

    internal fun update(next: NativeCheckInDetailPresentation, actions: CommunityCardActions) {
        if (!card.current().active) return
        data = next.takeIf { it.active }
        if (data == null) card.revoke() else card.update(next.card, actions)
    }

    fun close() {
        if (disposed) return
        disposed = true
        data = null
        screenVisible.value = false
        overlays?.close()
        card.close()
        scope.cancel()
        val release = onClose
        onClose = {}
        onVisibility = {}
        release()
    }
}

class IosCheckInDetailObservation internal constructor(private val observation: IosCommunityCardObservation) {
    fun cancel() = observation.cancel()
}
