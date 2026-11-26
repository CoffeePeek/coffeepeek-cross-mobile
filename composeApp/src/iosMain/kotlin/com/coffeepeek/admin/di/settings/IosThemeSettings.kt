package com.coffeepeek.admin.di.settings

import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.feature.settings.api.ThemePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** SwiftUI and Compose read and update the same app theme through [ThemePort]. */
class IosThemeSettings internal constructor(private val theme: ThemePort) {
    fun currentMode(): ThemeMode = theme.current().toApp()

    fun selectMode(mode: ThemeMode) = theme.set(mode.toSettings())

    /** Replays the current mode and delivers subsequent updates on the iOS main thread. */
    fun observeMode(onMode: (ThemeMode) -> Unit): IosThemeObservation {
        val job = CoroutineScope(Dispatchers.Main).launch {
            theme.observe().collect { onMode(it.toApp()) }
        }
        return IosThemeObservation(job)
    }
}

class IosThemeObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
