package com.coffeepeek.admin.di.profile

import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.impl.createProfileSnapshotStore
import org.koin.dsl.module

internal fun profileSnapshotModule() = module {
    single<ProfileSnapshotStore> { createProfileSnapshotStore() }
}
