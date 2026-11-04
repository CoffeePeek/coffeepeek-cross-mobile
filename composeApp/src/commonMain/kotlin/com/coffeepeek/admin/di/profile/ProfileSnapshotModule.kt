package com.coffeepeek.admin.di.profile

import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.impl.createProfileSnapshotStore
import com.coffeepeek.admin.ui.screen.profile.PersonalProfileWeekCache
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import org.koin.dsl.onClose

internal fun profileSnapshotModule() = module {
    single<ProfileSnapshotStore> { createProfileSnapshotStore() }
    single { PersonalProfileWeekCache(get<SessionRepository>(), CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) } onClose { it?.close() }
}
