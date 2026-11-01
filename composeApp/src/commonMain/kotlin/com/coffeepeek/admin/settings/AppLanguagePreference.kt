package com.coffeepeek.admin.settings

import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** App-owned bridge: load before rendering, then persist before applying each selection. */
internal class AppLanguagePreference(
    private val repository: SettingRepository,
    private val scope: CoroutineScope,
    private val applyLanguage: (String) -> Unit,
) : LanguagePort {
    private val mutex = Mutex()
    private val selected = MutableStateFlow<AppLanguage?>(null)
    val selection = selected.asStateFlow()

    init { scope.launch { load() } }

    override fun current() = selected.value ?: AppLanguage.RUSSIAN
    override fun observe() = selection.filterNotNull()

    override suspend fun set(language: AppLanguage): Result<Unit> = mutex.withLock {
        if (selected.value == language) return@withLock Result.success(Unit)
        try {
            repository.save(Setting(LANGUAGE_KEY, language.languageTag))
            publish(language)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }

    private suspend fun load() = mutex.withLock {
        val language = try {
            val tag = repository.read(LANGUAGE_KEY)?.value
            AppLanguage.entries.firstOrNull { it.languageTag == tag } ?: AppLanguage.RUSSIAN
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AppLanguage.RUSSIAN
        }
        publish(language)
    }

    private suspend fun publish(language: AppLanguage) = withContext(scope.coroutineContext.minusKey(Job)) {
        applyLanguage(language.languageTag)
        selected.value = language
    }

    internal companion object { const val LANGUAGE_KEY = "app_language" }
}
