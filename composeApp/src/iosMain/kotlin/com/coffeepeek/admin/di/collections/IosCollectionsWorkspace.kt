@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.impl.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.NSData
import platform.Foundation.create

/** Native view actions adapt to the feature session; no account tokens or DTOs cross the boundary. */
class IosCollectionsWorkspace internal constructor(
    private val session: CollectionsWorkspaceSession?,
    private val snapshot: PublicCollectionSession? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    fun current(): IosCollectionsWorkspaceState = (session?.current() ?: checkNotNull(snapshot).current()).iosState()
    fun poster(): IosCollectionsPoster? = (session?.poster() ?: snapshot?.poster())?.iosPoster()

    fun observe(onState: () -> Unit, onExport: (IosCollectionsPoster, Int) -> Unit): IosCollectionsWorkspaceObservation {
        val receive: (CollectionsPoster, Int) -> Unit = { poster, epoch -> onExport(poster.iosPoster(), epoch) }
        return IosCollectionsWorkspaceObservation(session?.observe(onState, receive) ?: checkNotNull(snapshot).observe(onState, receive))
    }

    fun reload() { session?.reload() ?: snapshot?.reload() }
    fun back() { session?.back() ?: snapshot?.back() }
    fun signIn() { session?.signIn() }
    fun title(value: String) { session?.title(value) }
    fun editDescription(value: String) { session?.description(value) }
    fun visibility(value: Boolean) { session?.visibility(value) }
    fun save() { session?.save() }
    fun delete() { session?.delete() }
    fun removeCover() { session?.removeCover() }
    fun feature(id: String?) { session?.feature(id) }
    fun togglePlace(key: String) { session?.togglePlace(key) }
    fun movePlace(key: String, target: String, after: Boolean) { session?.movePlace(key, target, after) }
    fun openCollection(id: String) { session?.openCollection(id) }
    fun openPlace(key: String) { session?.openPlace(key) ?: snapshot?.openPlace(key) }
    fun preview() { session?.preview() }
    fun shareLink() { session?.shareLink() ?: snapshot?.shareLink() }
    fun shareImages() { session?.shareImages() ?: snapshot?.shareImages() }
    fun format(story: Boolean) { session?.format(story) ?: snapshot?.format(story) }

    fun coverPicked(data: NSData, version: Int) {
        val session = session ?: return
        session.coverPreparing(true, version)
        scope.launch {
            val bytes = data.bytes?.readBytes(data.length.toInt()) ?: ByteArray(0)
            var prepared: ByteArray? = null
            try {
                prepared = withContext(Dispatchers.Default) { prepareCollectionCover(bytes) }
                session.coverPicked(prepared, version)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { session.coverPicked(ByteArray(0), version) }
            finally { bytes.fill(0); prepared?.fill(0); session.coverPreparing(false, version) }
        }
    }

    fun loadCover(id: String, version: Int, receive: (NSData?, String?) -> Unit): IosCollectionsWorkspaceObservation {
        val job = session?.loadCover(id, version) { bytes, url -> receive(bytes?.iosData(), url) }
            ?: CollectionsLibraryObservation { }
        return IosCollectionsWorkspaceObservation(job)
    }

    fun loadPhoto(url: String, receive: (NSData?) -> Unit): IosCollectionsWorkspaceObservation {
        val callback: (ByteArray?) -> Unit = { receive(it?.iosData()) }
        return IosCollectionsWorkspaceObservation(session?.loadPhoto(url, callback) ?: checkNotNull(snapshot).loadPhoto(url, callback))
    }

    fun finishExport(epoch: Int, id: String, images: List<NSData>) {
        val current = session?.isCurrentExport(epoch, id) ?: snapshot?.isCurrentExport(epoch, id) ?: false
        if (!current) return
        val bytes = images.mapNotNull { data -> data.bytes?.readBytes(data.length.toInt()) }
        var failed = bytes.isEmpty() || bytes.size != images.size
        try {
            if (!failed) {
                val poster = poster()
                ShareHelper.shareImages(bytes, listOfNotNull(poster?.title, poster?.url?.takeIf(String::isNotBlank)).joinToString("\n"))
            }
        } catch (_: Exception) { failed = true }
        finally {
            bytes.forEach { it.fill(0) }
            session?.finishExport(epoch, failed) ?: snapshot?.finishExport(epoch, id, failed)
        }
    }

    fun close() { scope.cancel(); session?.close(); snapshot?.close() }
}

class IosCollectionsWorkspaceObservation internal constructor(private val observation: CollectionsLibraryObservation) {
    fun cancel() = observation.cancel()
}

private fun ByteArray.iosData(): NSData? = takeIf { it.isNotEmpty() }?.usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

