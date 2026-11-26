@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.collections

import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.create

/** Native cover delivery uses the same revision-aware Kotlin photo source as other collection readers. */
internal class IosCollectionCoverImages(private val photos: CollectionsRepository, private val scope: CoroutineScope) {
    fun deliver(bytes: ByteArray?, url: String?, key: String?, current: () -> Boolean,
        receive: (NSData?, String?) -> Unit): Job? {
        if (bytes != null || url == null || key == null) {
            if (current()) receive(bytes?.coverData(), url)
            return null
        }
        return scope.launch { load(url, key, current, receive) }
    }

    private suspend fun load(url: String, key: String, current: () -> Boolean, receive: (NSData?, String?) -> Unit) {
        var bytes: ByteArray? = null
        try {
            bytes = photos.photo(url, key)
            currentCoroutineContext().ensureActive()
            if (current()) receive(bytes?.coverData(), null)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (current()) receive(null, null) }
        finally { bytes?.fill(0) }
    }
}

private fun ByteArray.coverData(): NSData? = takeIf { it.isNotEmpty() }?.usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}
