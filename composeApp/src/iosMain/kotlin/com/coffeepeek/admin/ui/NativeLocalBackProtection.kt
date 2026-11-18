package com.coffeepeek.admin.ui

import platform.UIKit.UIViewController

/** A native child with an in-screen back step temporarily protects the root edge-pop gesture. */
object NativeLocalBackProtection {
    fun update(controller: UIViewController, protected: Boolean) {
        controller.nativeScreenHost()?.handlesBackLocally = protected
    }
}
