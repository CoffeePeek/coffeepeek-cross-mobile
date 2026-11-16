package com.coffeepeek.admin.di.auth

import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Swift owns this screen and its presentation until UIKit removes the route. */
sealed class IosAuthScreen(
    private val language: LanguagePort,
    private val onBack: () -> Unit,
    private val closePresentation: () -> Unit,
) {
    internal val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun languageTag() = language.current().languageTag
    fun observeLanguage(onLanguage: (String) -> Unit) = observe(language.observe().map { it.languageTag }, onLanguage)
    fun back() = onBack()

    fun close() {
        scope.cancel()
        closePresentation()
    }

    internal fun <T> observe(flow: Flow<T>, callback: (T) -> Unit): IosAuthObservation =
        IosAuthObservation(scope.launch { flow.collect { callback(it) } })
}

class IosAuthObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
