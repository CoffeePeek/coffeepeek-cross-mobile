package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.media.IosPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.roaster.impl.NativeRoasterDetailHost
import com.coffeepeek.feature.roaster.impl.NativeRoasterDetailSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController

class IosRoasterDetail internal constructor(
    private val session: NativeRoasterDetailSession,
    private var host: NativeRoasterDetailHost,
    language: LanguagePort, accounts: SessionReader, private val media: IosCatalogueMedia,
    private val ownership: () -> UIViewController?,
) : IosRoasterScreen(language, accounts, media::observeImage) {
    fun current(): IosRoasterDetailState {
        val state = session.current()
        val details = state.details.takeIf { isCurrent() }
        return IosRoasterDetailState(isCurrent(), state.loading, state.failed, details?.id, details?.name,
            details?.about, details?.location?.address, state.instagram.takeIf { isCurrent() }, state.website.takeIf { isCurrent() },
            details?.photos?.map { it.url }.orEmpty(), details?.shops?.map { IosRoasterShop(it.slug, it.name, it.photoUrl) }.orEmpty(),
            state.favorite, state.photo.takeIf { isCurrent() }, state.messageKey.takeIf { isCurrent() }, state.message.takeIf { isCurrent() })
    }
    fun observeState(receive: (IosRoasterDetailState) -> Unit) = observe(session.state) { receive(current()) }
    fun observeEvents() = IosRoasterObservation(scope.launch {
        session.events.collect { event -> guarded { when (event.kind) {
            "back" -> host.onBack()
            "shop" -> host.onOpenShop(event.value)
            "link" -> host.onOpenLink(event.value)
            else -> Unit
        } } }
    })
    fun retry() = guarded(session::retry)
    fun back() = guarded(session::back)
    fun favorite() = guarded { host.onFavoriteFeedback(); session.favorite() }
    fun photo(url: String) = guarded { session.photo(url) }
    fun closePhoto() = guarded(session::closePhoto)
    fun shop(slug: String) = guarded { session.shop(slug) }
    fun instagram() = guarded(session::instagram)
    fun website() = guarded(session::website)
    fun dismissMessage() = guarded(session::dismissMessage)
    fun share() = guarded { session.current().details?.address?.canonicalPath?.let(host.onShare) }
    fun makeOwnershipActions() = if (isCurrent()) ownership() else null
    fun makePhotoViewer(): IosPhotoViewer? {
        val url = current().photo ?: return null
        return media.viewer(CoffeePeekPhotoViewerImages(listOf(url)), 0) { isCurrent() && session.current().photo == url }
    }
    internal fun update(next: NativeRoasterDetailHost) { host = next }
}

data class IosRoasterShop(val slug: String, val name: String, val photo: String?)
data class IosRoasterDetailState(
    val active: Boolean, val loading: Boolean, val failed: Boolean, val id: String?, val name: String?,
    val about: String?, val address: String?, val instagram: String?, val website: String?,
    val photos: List<String>, val shops: List<IosRoasterShop>, val favorite: Boolean,
    val photo: String?, val messageKey: String?, val message: String?,
)
