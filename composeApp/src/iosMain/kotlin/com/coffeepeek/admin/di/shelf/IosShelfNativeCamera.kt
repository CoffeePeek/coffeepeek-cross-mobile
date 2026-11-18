@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.coffeepeek.feature.shelf.api.ShelfCameraStatus
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIView

/** A SwiftUI viewfinder uses the same focus, torch, capture and image normalization as the existing camera port. */
class IosShelfNativeCamera internal constructor(scope: CoroutineScope, private val allowed: () -> Boolean,
    private val onPhoto: (ByteArray) -> Unit, private val onFailure: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Main)
    private val camera = IosShelfCameraSession(this.scope)
    private var capturing by mutableStateOf(false)
    private var closed = false
    init { camera.start() }
    fun view(): UIView = camera.nativeView()
    fun current() = IosShelfCameraState(camera.status.name, camera.torchAvailable, camera.torch, capturing,
        camera.focus?.x, camera.focus?.y, camera.focus?.sharp)
    fun observe(receive: (IosShelfCameraState) -> Unit) = IosShelfObservation(listOf(scope.launch {
        snapshotFlow { current() }.collect { receive(it) }
    }))
    fun torch() { if (!closed && allowed()) camera.setTorch(!camera.torch) }
    fun focus(x: Float, y: Float) { if (!closed && allowed()) camera.focusAt(x, y) }
    fun allow() { if (!closed && allowed()) camera.requestAccess() }
    fun capture() {
        if (closed || !allowed() || capturing || camera.status != ShelfCameraStatus.Ready) return
        capturing = true
        scope.launch {
            val photo = camera.capture()
            capturing = false
            if (!closed && allowed()) { if (photo == null) onFailure() else onPhoto(photo.bytes) }
        }
    }
    fun close() { if (!closed) { closed = true; camera.stop(); scope.cancel() } }
}

data class IosShelfCameraState(val status: String, val torchAvailable: Boolean, val torch: Boolean,
    val capturing: Boolean, val focusX: Float?, val focusY: Float?, val sharp: Boolean?)

internal fun ByteArray.toShelfData(): NSData? = if (isEmpty()) null else usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}
