@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.collections

import com.coffeepeek.feature.collections.api.CollectionsLibrarySession
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.api.CollectionsLibraryEvent
import com.coffeepeek.feature.collections.api.CollectionsLibraryItem
import com.coffeepeek.feature.collections.api.CollectionsLibrarySnapshot
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIViewController

/** Swift sees presentation values only; account access and media authorization stay in Kotlin. */
class IosCollectionsLibrary internal constructor(
    private val session: CollectionsLibrarySession,
    private val onBack: () -> Unit,
    private val onSignIn: () -> Unit,
    private val onEditor: (String?) -> Unit,
    private val preferences: () -> UIViewController,
) {
    fun current(): IosCollectionsLibraryState = session.current().iosState()

    fun observe(onState: (IosCollectionsLibraryState) -> Unit): IosCollectionsLibraryObservation =
        IosCollectionsLibraryObservation(session.observe({ onState(current()) }) { event ->
            when (event) {
                CollectionsLibraryEvent.Back -> onBack()
                CollectionsLibraryEvent.SignIn -> onSignIn()
                is CollectionsLibraryEvent.Editor -> onEditor(event.id)
            }
        })

    fun reload() = session.reload()
    fun query(value: String) = session.query(value)
    fun openEditor(id: String?) = session.openEditor(id)
    fun signIn() = session.signIn()
    fun back() = session.back()
    fun close() = session.close()
    fun makePreferences(): UIViewController? = if (session.current().signedIn) preferences() else null

    fun loadCover(id: String, accountVersion: Int,
        onCover: (NSData?, String?) -> Unit): IosCollectionsLibraryObservation =
        IosCollectionsLibraryObservation(session.loadCover(id, accountVersion) { bytes, url ->
            val data = bytes?.takeIf { it.isNotEmpty() }?.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
            onCover(data, url)
        })
}

class IosCollectionsLibraryObservation internal constructor(private val observation: CollectionsLibraryObservation) {
    fun cancel() = observation.cancel()
}

data class IosCollectionsLibraryState(
    val accountVersion: Int,
    val signedIn: Boolean,
    val loading: Boolean,
    val refreshing: Boolean,
    val failed: Boolean,
    val query: String,
    val items: List<IosCollectionsLibraryItem>,
    val matchingItems: List<IosCollectionsLibraryItem> = items,
)

private fun CollectionsLibrarySnapshot.iosState() = IosCollectionsLibraryState(
    accountVersion, signedIn, loading, refreshing, failed, query,
    items.map { it.iosItem() }, matchingItems.map { it.iosItem() },
)

private fun CollectionsLibraryItem.iosItem() = IosCollectionsLibraryItem(id, title, isPublic, count, imageKey)

data class IosCollectionsLibraryItem(
    val id: String,
    val title: String,
    val isPublic: Boolean,
    val count: Int,
    val imageKey: String,
)
