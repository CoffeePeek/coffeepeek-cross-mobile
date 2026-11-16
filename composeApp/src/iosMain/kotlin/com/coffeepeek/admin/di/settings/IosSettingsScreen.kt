package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Swift owns one instance for the lifetime of its UIKit route. */
sealed class IosSettingsScreen(
    private val language: LanguagePort,
    private val onBack: () -> Unit,
    private val closePresentation: () -> Unit,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun languageTag() = language.current().languageTag
    fun observeLanguage(onLanguage: (String) -> Unit) = observe(language.observe().map { it.languageTag }, onLanguage)
    fun back() = onBack()

    fun close() {
        scope.cancel()
        closePresentation()
    }

    internal fun <T> observe(flow: Flow<T>, onState: (T) -> Unit): IosSettingsObservation =
        IosSettingsObservation(scope.launch { flow.collect { onState(it) } })

    internal fun perform(action: suspend () -> Unit) {
        scope.launch { action() }
    }
}

class IosSettingsObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
