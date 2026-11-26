package com.coffeepeek.admin.ui

import kotlinx.coroutines.Job

/** Opaque entry identity. Swift owns controllers and UIKit transitions; routes stay in Kotlin. */
class IosRootRoute internal constructor(val generation: Long, val key: Long, internal val screen: Navigator.Screen,
    val page: String, val visit: String, val nativeNavigationBar: Boolean, val lightStatusBar: Boolean, val localBack: Boolean)
data class IosRootStack(val index: Int, val routes: List<IosRootRoute>)
data class IosRootState(val ready: Boolean = false, val failed: Boolean = false, val generation: Long = 0,
    val command: Long = 0, val selected: Int = 0, val stacks: List<IosRootStack> = emptyList(),
    val visibleTabs: List<Int> = emptyList(), val labels: List<String> = emptyList(), val createTitle: String = "",
    val theme: String = "SYSTEM", val expandedMap: Boolean = false, val failure: String = "", val retry: String = "")
class IosRootObservation internal constructor(private val job: Job) { fun cancel() = job.cancel() }
