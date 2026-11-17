@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.collections

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.impl.SavedPublicCollectionSession
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIViewController

class IosSavedPublicCollection internal constructor(private val session: SavedPublicCollectionSession) {
    fun current(): IosSavedPublicCollectionState = session.current().let { state ->
        IosSavedPublicCollectionState(
            state.loading, state.failed, state.busy, state.preview, state.title, state.description,
            state.coverKey, state.canShare,
            state.places.map { IosSavedPublicPlace(it.key, it.title, it.address, it.roaster, it.photoUrl) },
        )
    }
    fun observe(onState: () -> Unit): IosSavedPublicCollectionObservation =
        IosSavedPublicCollectionObservation(session.observe(onState))

    fun back() = session.back()
    fun reload() = session.reload()
    fun preview() = session.preview()
    fun shareLink() = session.shareLink()
    fun openPlace(key: String) = session.openPlace(key)
    fun close() = session.close()

    fun makePoster(): UIViewController = ComposeUIViewController {
        AppContent { session.Poster(::decodeCollectionPosterPhoto) }
    }

    fun loadCover(key: String, onCover: (NSData?, String?) -> Unit): IosSavedPublicCollectionObservation =
        IosSavedPublicCollectionObservation(session.loadCover(key) { bytes, url ->
            val data = bytes?.takeIf { it.isNotEmpty() }?.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
            onCover(data, url)
        })
}

class IosSavedPublicCollectionObservation internal constructor(private val observation: CollectionsLibraryObservation) {
    fun cancel() = observation.cancel()
}

data class IosSavedPublicCollectionState(
    val loading: Boolean,
    val failed: Boolean,
    val busy: Boolean,
    val preview: Boolean,
    val title: String?,
    val description: String,
    val coverKey: String?,
    val canShare: Boolean,
    val places: List<IosSavedPublicPlace>,
)

data class IosSavedPublicPlace(
    val key: String,
    val title: String,
    val address: String,
    val roaster: Boolean,
    val photoUrl: String?,
)
