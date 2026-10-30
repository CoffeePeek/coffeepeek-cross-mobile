package com.coffeepeek.admin.di

import com.coffeepeek.core.session.*
import com.coffeepeek.data.di.dataModule
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.*

class SessionPortsWiringTest {
    @Test fun actualDataModuleAliasesShareOneStoreWithoutReadingPlatformStorage() = runBlocking {
        val database = object : DatabaseCore {
            override val settingRepository: SettingRepository get() = error("Room must stay lazy")
        }
        val app = koinApplication {
            modules(dataModule("https://unused.example", database), offlineStorage())
        }
        try {
            val reader = app.koin.get<SessionReader>()
            val store = app.koin.get<SessionStore>()
            assertSame(store, reader)
            assertSame(store, app.koin.get<SessionStore>())
            assertNull(reader.peekSession())
            val session = Session("in-memory-token", userId = "A")
            store.applySession(session)
            assertEquals(session, reader.peekSession())
            assertEquals(session, reader.observeSession().first())
        } finally { app.close() }
    }

    private fun offlineStorage() = module {
        single<SessionSecureStore> {
            object : SessionSecureStore {
                override suspend fun read(): SessionCredentials? = error("No secure storage reads")
                override suspend fun write(data: SessionCredentials?) = error("No secure storage writes")
                override fun observe() = kotlinx.coroutines.flow.flowOf<SessionCredentials?>(null)
            }
        }
        single<LegacyCredentialRows> {
            object : LegacyCredentialRows {
                override suspend fun read(key: String): SessionCredentials? = error("No legacy reads")
                override suspend fun delete(key: String) = error("No legacy deletion")
            }
        }
    }
}
