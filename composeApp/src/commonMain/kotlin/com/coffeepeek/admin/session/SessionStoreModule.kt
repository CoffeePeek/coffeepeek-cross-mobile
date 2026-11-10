package com.coffeepeek.admin.session

import com.coffeepeek.core.session.DefaultSessionStore
import com.coffeepeek.core.session.LegacyCredentialRows
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.core.session.createPlatformSessionSecureStore
import com.coffeepeek.room.DatabaseCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/** The account's stored session: secure storage, the one-time import of old Room rows, and the store everyone reads. */
internal fun sessionStoreModule(database: DatabaseCore, platformContext: Any?) = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    single<LegacyCredentialRows> { RoomLegacyCredentialRows(database.settingRepository) }
    single { createPlatformSessionSecureStore(platformContext, get()) }
    single<SessionStore> { DefaultSessionStore(get(), get()) }
    single<SessionReader> { get<SessionStore>() }
}
