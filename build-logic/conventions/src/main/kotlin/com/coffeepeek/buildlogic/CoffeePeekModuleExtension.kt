package com.coffeepeek.buildlogic

import com.android.build.gradle.LibraryExtension

/** Small per-module surface; toolchain and platform defaults live in convention plugins. */
open class CoffeePeekModuleExtension {
    private var androidExtension: LibraryExtension? = null

    var androidNamespace: String = ""
        set(value) {
            field = value
            androidExtension?.namespace = value
        }

    var resourcePrefix: String = ""
        set(value) {
            field = value
            if (value.isNotBlank()) androidExtension?.resourcePrefix = value
        }

    internal fun attach(android: LibraryExtension) {
        androidExtension = android
        if (androidNamespace.isNotBlank()) android.namespace = androidNamespace
        if (resourcePrefix.isNotBlank()) android.resourcePrefix = resourcePrefix
    }
}
