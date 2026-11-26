@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.feature.homecheckin.impl.ui

import org.jetbrains.compose.resources.*

internal class NativeHomeDetailResources {
    private val environment = getSystemResourceEnvironment()
    suspend fun text(resource: StringResource, vararg args: Any) =
        if (args.isEmpty()) getString(environment, resource) else getString(environment, resource, *args)
}
