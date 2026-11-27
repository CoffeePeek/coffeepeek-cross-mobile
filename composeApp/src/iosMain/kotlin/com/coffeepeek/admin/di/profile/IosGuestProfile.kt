package com.coffeepeek.admin.di.profile

import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Only native navigation and localization; the signed-in profile keeps its feature entry. */
class IosGuestProfile internal constructor(
    private val language: LanguagePort,
    private val onLogin: () -> Unit,
    private val onRegister: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun languageTag() = language.current().languageTag

    fun observeLanguage(onLanguage: (String) -> Unit) = IosGuestProfileObservation(
        scope.launch { language.observe().collect { onLanguage(it.languageTag) } },
    )

    fun login() = onLogin()
    fun register() = onRegister()
    fun close() = scope.cancel()
}

class IosGuestProfileObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
