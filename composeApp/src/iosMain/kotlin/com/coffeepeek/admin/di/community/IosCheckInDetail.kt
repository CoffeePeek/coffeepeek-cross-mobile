package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityCardActions
import platform.UIKit.UIImage

/** Full-screen adapter shares the card's account lease, image permissions and callback guards. */
class IosCheckInDetail internal constructor(
    private var data: NativeCheckInDetailPresentation?, actions: CommunityCardActions,
    media: IosCatalogueMedia, accounts: SessionReader,
) {
    private val card = IosCommunityCard(data?.card, actions, media, accounts)
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

    internal fun close() {
        data = null
        card.close()
    }
}

class IosCheckInDetailObservation internal constructor(private val observation: IosCommunityCardObservation) {
    fun cancel() = observation.cancel()
}
