@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.utils

import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

actual object ShareHelper {
    actual fun shareText(text: String) {
        presentShare(listOf(text))
    }

    actual fun shareImages(images: List<ByteArray>, text: String) {
        require(images.isNotEmpty() && images.all { it.isNotEmpty() })
        val directory = "${NSHomeDirectory()}/Library/Caches/shared-collections"
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
        removeExpiredCollectionImages(directory)
        val batch = NSUUID().UUIDString
        val files = images.mapIndexed { index, bytes ->
            val path = "$directory/$batch-${index + 1}.png"
            bytes.usePinned { pinned ->
                check(NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()).writeToFile(path, true))
            }
            NSURL.fileURLWithPath(path)
        }
        check(presentShare(files + text))
    }
}

private fun presentShare(items: List<Any>): Boolean {
    val presenter = topViewController() ?: return false
    val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
    controller.overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
    controller.popoverPresentationController?.apply {
        sourceView = presenter.view
        sourceRect = presenter.view.bounds
        permittedArrowDirections = 0u
    }
    presenter.presentViewController(controller, animated = true, completion = null)
    return true
}

private fun removeExpiredCollectionImages(directory: String) {
    val manager = NSFileManager.defaultManager
    val cutoff = NSDate().timeIntervalSince1970 - 172_800.0
    manager.contentsOfDirectoryAtPath(directory, null)?.filterIsInstance<String>()?.filter { it.endsWith(".png") }?.forEach { name ->
        val path = "$directory/$name"
        val modified = manager.attributesOfItemAtPath(path, null)?.get(NSFileModificationDate) as? NSDate
        if (modified != null && modified.timeIntervalSince1970 < cutoff) manager.removeItemAtPath(path, null)
    }
}

private fun topViewController(): UIViewController? {
    var current = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (current?.presentedViewController != null) {
        current = current.presentedViewController
    }
    return current
}
