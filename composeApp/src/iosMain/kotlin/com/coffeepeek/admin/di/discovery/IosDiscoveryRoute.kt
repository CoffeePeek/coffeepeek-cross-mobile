@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.admin.di.discovery

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import platform.UIKit.UIViewController

object IosDiscoveryRouteControllers {
    private lateinit var factory: (IosDiscoveryRoute) -> UIViewController
    fun configure(factory: (IosDiscoveryRoute) -> UIViewController) { this.factory = factory }
    internal fun controller(route: IosDiscoveryRoute) = factory(route)
}

/** Retry replaces the entire failed account observation, leaving no stale map/search owner. */
class IosDiscoveryRoute internal constructor(language: LanguagePort, private val create: ((Boolean) -> Unit) -> IosDiscovery,
    private val mapExpanded: (Boolean) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val revision = MutableStateFlow(0)
    private val output = MutableStateFlow(IosDiscoveryRouteState())
    private var closed = false

    init {
        scope.launch { language.observe().collectLatest {
            val environment = getSystemResourceEnvironment()
            val failure = getString(environment, Res.string.shop_session_unavailable)
            val retry = getString(environment, Res.string.shop_session_retry)
            output.update { it.copy(failure = failure, retry = retry) }
        } }
        scope.launch { revision.collectLatest(::bind) }
    }
    fun current() = output.value
    fun observe(receive: (IosDiscoveryRouteState) -> Unit) = IosDiscoveryObservation(scope.launch { output.collect(receive) })
    fun retry() { if (!closed && output.value.failed) revision.value++ }
    fun close() {
        if (closed) return
        closed = true
        output.value.discovery?.close()
        output.value = output.value.copy(discovery = null)
        scope.cancel()
        mapExpanded(false)
    }
    private suspend fun bind(generation: Int) {
        output.update { it.copy(discovery = null, failed = false) }
        val source = create { if (!closed && revision.value == generation) mapExpanded(it) }
        output.update { it.copy(discovery = source) }
        try {
            source.failed.first { it }
            output.update { if (it.discovery === source) it.copy(discovery = null, failed = true) else it }
        } finally {
            source.close()
            output.update { if (it.discovery === source) it.copy(discovery = null) else it }
        }
    }
}

data class IosDiscoveryRouteState(val discovery: IosDiscovery? = null, val failed: Boolean = false,
    val failure: String = "", val retry: String = "")
