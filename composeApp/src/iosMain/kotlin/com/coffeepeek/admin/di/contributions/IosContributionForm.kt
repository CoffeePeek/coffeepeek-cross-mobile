@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.impl.NativeContributionFormSession
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

class IosContributionForm internal constructor(
    private val session: NativeContributionFormSession,
    private val slug: String,
    private val media: IosCatalogueMedia,
) : IosContributionsRoute() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val map = IosContributionMap(session)

    fun current(): String = session.current().iosJson()
    fun observe(onState: () -> Unit) = IosContributionObservation(session.observe(onState))
    fun edit(field: String, value: String) = session.edit(field, value)
    fun toggle(group: String, id: String) = session.toggle(group, id)
    fun control(action: String) = session.control(action)
    fun schedule(action: String, day: Int, value: Int) = session.schedule(action, day, value)
    fun menu(slug: String, availability: String?, price: String?) = session.menu(slug, availability, price)
    fun beginPhotoPick(menu: Boolean): Int = session.beginPhotoPick(menu)
    fun removePhoto(key: String) = session.removePhoto(key)

    fun formBack() {
        if (!session.consumeBack()) Navigator.popBack()
    }

    fun selectSection(value: String) {
        val section = session.selectedSection(value) ?: return
        Navigator.navigate(Navigator.Screen.ShopChangeEditor(shopId = slug, section = section.name))
    }

    fun addRoaster() {
        if (session.current().signedIn == 1) Navigator.navigate(Navigator.Screen.AddRoaster)
    }

    fun openRequest() {
        session.current().requestId?.let { Navigator.popThenNavigate(Navigator.Screen.ShopChangeRequestDetail(it)) }
    }

    fun photosPicked(revision: Int, data: List<NSData>, onComplete: () -> Unit) {
        scope.launch {
            try {
                val photos = withContext(Dispatchers.Default) { data.mapNotNull(::contributionPhoto) }
                session.pickedPhotos(revision, photos)
            } finally { onComplete() }
        }
    }

    fun loadPhoto(key: String, version: Int, receive: (NSData?) -> Unit): IosContributionObservation =
        IosContributionObservation(scope.launch {
            val bytes = session.localPhoto(key, version)
            try { receive(bytes?.asNativeData()) } finally { bytes?.fill(0) }
        })

    fun cameraPicked(revision: Int, image: UIImage, onComplete: () -> Unit) {
        scope.launch {
            try {
                val photo = withContext(Dispatchers.Default) { contributionPhoto(image) }
                session.pickedPhotos(revision, listOfNotNull(photo))
            } finally { onComplete() }
        }
    }

    fun loadImage(url: String, version: Int, receive: (UIImage?) -> Unit): IosContributionObservation =
        IosContributionObservation(scope.launch {
            val image = media.loadImage(url)
            currentCoroutineContext().ensureActive()
            if (session.current().accountVersion == version) receive(image)
        })

    fun locationGranted(map: Boolean) = session.locationGranted(map)
    fun locationDenied() = session.locationDenied()
    fun mapMove(latitude: Double, longitude: Double) = session.moveMap(latitude, longitude)
    fun cameraApplied() = session.cameraApplied()
    fun feedback(success: Boolean) = platformHaptic(if (success) HapticEvent.Success else HapticEvent.Error)

    override fun close() {
        scope.cancel()
        session.close()
    }
}

private fun contributionPhoto(data: NSData): PendingPhoto? {
    return try { contributionPhoto(UIImage(data = data)) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
}

private fun contributionPhoto(image: UIImage): PendingPhoto? {
    val encoded = UIImageJPEGRepresentation(image, 0.9) ?: return null
    val bytes = encoded.bytes?.readBytes(encoded.length.toInt()) ?: return null
    return PendingPhoto("photo-${NSUUID().UUIDString}.jpg", "image/jpeg", bytes)
}

private fun ByteArray.asNativeData(): NSData? = takeIf { it.isNotEmpty() }?.usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

