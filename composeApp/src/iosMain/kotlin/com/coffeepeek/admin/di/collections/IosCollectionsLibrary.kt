@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.collections

import com.coffeepeek.feature.collections.api.CollectionsLibrarySession
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.api.CollectionsLibraryEvent
import com.coffeepeek.feature.collections.api.CollectionsLibraryItem
import com.coffeepeek.feature.collections.api.CollectionsLibrarySnapshot
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import platform.Foundation.NSData

/** Swift sees presentation values only; account access and media authorization stay in Kotlin. */
class IosCollectionsLibrary internal constructor(
    private val session: CollectionsLibrarySession,
    private val onBack: () -> Unit,
    private val onSignIn: () -> Unit,
    private val onEditor: (String?) -> Unit,
    photos: CollectionsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val images = IosCollectionCoverImages(photos, scope)
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
    fun close() { scope.cancel(); session.close() }

    fun loadCover(id: String, accountVersion: Int,
        onCover: (NSData?, String?) -> Unit): IosCollectionsLibraryObservation {
        val key = coverKey(id, accountVersion)
        var image: Job? = null
        val request = session.loadCover(id, accountVersion) { bytes, url ->
            image = images.deliver(bytes, url, key, { key != null && key == coverKey(id, accountVersion) }, onCover)
        }
        return IosCollectionsLibraryObservation(CollectionsLibraryObservation { request.cancel(); image?.cancel() })
    }

    private fun coverKey(id: String, version: Int): String? = current()
        .takeIf { it.signedIn && it.accountVersion == version }?.items?.firstOrNull { it.id == id }?.imageKey
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
