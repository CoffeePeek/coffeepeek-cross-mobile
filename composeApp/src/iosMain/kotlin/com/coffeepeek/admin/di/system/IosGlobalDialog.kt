@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.system

import com.coffeepeek.core.designsystem.component.CoffeePeekLoaderGeometry
import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

class IosGlobalDialog internal constructor(
    val error: Boolean, val title: String, val message: String?, val closeLabel: String,
    private val onDismiss: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var closed = false
    fun dismiss() { if (!closed && error) onDismiss() }
    fun loadIllustration(receive: (UIImage?) -> Unit) = IosGlobalDialogObservation(scope.launch {
        val bytes = CoffeePeekIllustrations.loadErrorBytes()
        if (!closed && bytes.isNotEmpty()) bytes.usePinned {
            receive(UIImage.imageWithData(NSData.create(it.addressOf(0), bytes.size.toULong())))
        }
    })
    internal fun close() {
        closed = true
        scope.cancel()
    }
}

object IosLoaderGeometry {
    val contours = CoffeePeekLoaderGeometry.contours.map { shape -> shape.map { IosLoaderPoint(it.x, it.y) } }
    val size = CoffeePeekDimens.loaderDefault.value
    val stroke = (size / 16f).coerceAtLeast(2f)
    val rotationSeconds = CoffeePeekLoaderGeometry.rotationDurationMillis / 1000.0
    val fillSeconds = CoffeePeekLoaderGeometry.fillDurationMillis / 1000.0
    val morphSeconds = CoffeePeekLoaderGeometry.morphDurationMillis / 1000.0
    val maxFill = CoffeePeekLoaderGeometry.maxFillFraction
}
data class IosLoaderPoint(val x: Float, val y: Float)

class IosGlobalDialogObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
