package com.coffeepeek.admin.di.media

/** A modal lifetime around the existing protected image source. */
class IosCheckInPhotoPresentation internal constructor(
    val languageTag: String,
    private val current: () -> Boolean,
    private val onDismiss: () -> Unit,
    createViewer: (() -> Boolean) -> IosPhotoViewer,
) {
    private var closed = false
    val viewer = createViewer(::isCurrent)
    internal fun isCurrent() = !closed && current()
    fun dismiss() {
        if (closed) return
        close()
        onDismiss()
    }
    internal fun close() {
        closed = true
        viewer.setVisible(false)
    }
}
