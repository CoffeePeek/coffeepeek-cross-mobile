package com.coffeepeek.admin.di.settings

import com.coffeepeek.admin.settings.AppLanguagePreference
import com.coffeepeek.admin.settings.applyPlatformAppLanguage
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.api.LanguageEntry
import com.coffeepeek.feature.settings.impl.createLanguageEntry
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/** The app lifetime owns persistence and the platform locale, outside feature UI. */
internal fun appLanguageModule(repository: SettingRepository) = module {
    single {
        AppLanguagePreference(repository, CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()), ::applyPlatformAppLanguage)
    }
    single<LanguagePort> { get<AppLanguagePreference>() }
    single<LanguageEntry> { createLanguageEntry(get()) }
}
