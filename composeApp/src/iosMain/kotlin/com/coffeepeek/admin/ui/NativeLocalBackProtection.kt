package com.coffeepeek.admin.ui

import platform.UIKit.UIViewController

/** A native child with an in-screen back step temporarily protects the root edge-pop gesture. */
object NativeLocalBackProtection {
    private var applyUpdate: (UIViewController, Boolean) -> Unit = { _, _ -> }
    private var identity: (UIViewController) -> String? = { null }
    fun configure(update: (UIViewController, Boolean) -> Unit, identity: (UIViewController) -> String?) {
        this.applyUpdate = update
        this.identity = identity
    }
    fun update(controller: UIViewController, enabled: Boolean) {
        applyUpdate(controller, enabled)
    }
    internal fun visit(controller: UIViewController) = identity(controller)
}
