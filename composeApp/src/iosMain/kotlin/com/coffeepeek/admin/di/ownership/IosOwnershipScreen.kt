@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.ownership

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.feature.ownership.impl.IosOwnershipSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

class IosOwnershipScreen internal constructor(private val session: IosOwnershipSession,
    private val language: LanguagePort, private val media: IosCatalogueMedia) {
    val tab = session.tab
    val claimSheet = session.claimSheet
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var closed = false
    fun current() = session.current().iosState().let { state ->
        if (closed) state.copy(signedIn = false, claims = emptyList(), businesses = emptyList(), contact = "", evidence = "",
            evidencePhotos = emptyList(), claim = null, editorPhotos = emptyList(), fields = emptyList()) else state
    }
    fun languageTag() = language.current().languageTag
    fun observe(receive: (IosOwnershipState) -> Unit, onLanguage: (String) -> Unit): IosOwnershipObservation {
        if (closed) return IosOwnershipObservation(emptyList())
        return IosOwnershipObservation(listOf(session.observe { if (!closed) receive(current()) },
            scope.launch { language.observe().collect { onLanguage(it.languageTag) } }))
    }
    fun action(value: String, generation: Int) { if (!closed) session.action(value, generation) }
    fun claim(id: String, generation: Int) { if (!closed) session.claim(id, generation) }
    fun business(kind: String, id: String, generation: Int) { if (!closed) session.business(kind, id, generation) }
    fun claimsPage(page: Int, generation: Int) { if (!closed) session.claimsPage(page, generation) }
    fun roastersPage(page: Int, generation: Int) { if (!closed) session.roastersPage(page, generation) }
    fun claimText(field: String, value: String, generation: Int) { if (!closed) session.claimText(field, value, generation) }
    fun profileText(field: String, value: String, generation: Int) { if (!closed) session.profileText(field, value, generation) }
    fun choosePhotos(generation: Int) { if (!closed) session.choosePhotos(generation) }
    fun removePhoto(index: Int, evidence: Boolean, generation: Int) { if (!closed) session.removePhoto(index, evidence, generation) }
    fun movePhoto(from: Int, to: Int, evidence: Boolean, generation: Int) { if (!closed) session.movePhoto(from, to, evidence, generation) }
    fun qrLink(id: String, generation: Int) = if (closed) null else session.qrLink(id, generation)
    fun shareQr(image: UIImage, name: String, generation: Int) {
        if (!closed && session.isCurrent(generation)) UIImagePNGRepresentation(image)?.ownershipBytes()?.let { session.shareQr(it, name, generation) }
    }
    fun loadImage(key: String, generation: Int, receive: (UIImage?) -> Unit): IosOwnershipObservation {
        return IosOwnershipObservation(listOf(scope.launch {
            val source = if (closed) null else session.photo(key, generation)
            if (source?.bytes != null) {
                val image = withContext(Dispatchers.Default) { source.bytes?.ownershipImage() }
                if (!closed && session.isCurrent(generation)) receive(image)
            } else source?.url?.let { url ->
                media.observeImage(url) { !closed && session.isCurrent(generation) }.collect { receive(it) }
            }
        }))
    }
    fun close() { closed = true; scope.cancel() }
}

class IosOwnershipObservation internal constructor(private val jobs: List<Job>) { fun cancel() = jobs.forEach { it.cancel() } }

private fun ByteArray.ownershipImage(): UIImage? = if (isEmpty()) null else usePinned {
    UIImage.imageWithData(NSData.create(bytes = it.addressOf(0), length = size.toULong()))
}
private fun NSData.ownershipBytes(): ByteArray = ByteArray(length.toInt()).also { bytes ->
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}
