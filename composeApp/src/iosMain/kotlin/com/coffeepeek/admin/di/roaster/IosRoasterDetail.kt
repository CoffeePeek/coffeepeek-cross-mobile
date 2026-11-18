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
    private val ownership: (() -> Boolean) -> UIViewController?,
) : IosRoasterScreen(language, accounts, media::observeImage) {
    fun current(): IosRoasterDetailState {
        val state = session.current()
        val active = isCurrent()
        val details = state.details.takeIf { active }
        return IosRoasterDetailState(active, state.loading && active, state.failed && active, details?.id, details?.name,
            details?.about, details?.location?.address, state.instagram.takeIf { active }, state.website.takeIf { active },
            details?.photos?.map { it.url }.orEmpty(), details?.shops?.map { IosRoasterShop(it.slug, it.name, it.photoUrl) }.orEmpty(),
            state.favorite && active, state.photo.takeIf { active }, state.messageKey.takeIf { active }, state.message.takeIf { active })
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
    fun makeOwnershipActions() = if (isCurrent()) ownership(::isCurrent) else null
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
