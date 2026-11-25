@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.di.iosPlatformModules
import com.coffeepeek.admin.di.collections.IosCollectionsLibrary
import com.coffeepeek.feature.collections.api.*
import com.coffeepeek.feature.profile.api.ProfileSnapshot
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.error.NoDefinitionFoundException
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import platform.UIKit.UIViewController

@OptIn(KoinInternalApi::class)
class IosProfileCompositionTest {
    @Test fun missingProfileModuleReproducesTheReleaseDependencyFailure() {
        val oldModules = iosPlatformModules(BASE_URL).filterNot { candidate ->
            candidate.mappings.values.any { it.beanDefinition.primaryType == ProfileSnapshotStore::class }
        }
        withProfileGraph(oldModules) { koin ->
            assertFailsWith<NoDefinitionFoundException> { koin.get<ProfileSnapshotStore>() }
        }
    }

    @Test fun theProductionPlatformGraphProvidesTheProfileStoreAsASingleton() {
        withProfileGraph { koin -> assertSame(koin.get<ProfileSnapshotStore>(), koin.get<ProfileSnapshotStore>()) }
    }

    @Test fun theProfileStoreKeepsOneAccountScopeAndClears() {
        withProfileGraph { koin ->
            val store = koin.get<ProfileSnapshotStore>()
            val snapshot = ProfileSnapshot(PublicProfile("owner", "Owner", null, null, PublicProfileStats(0, 0, 0, 0), emptyList(), null),
                emptyList(), null, "2026-11")
            store.write("1:owner", "owner", 0, snapshot)
            assertEquals(snapshot, store.read("1:owner", "owner", 0))
            assertNull(store.read("2:owner", "owner", 0))
            store.clear()
            assertNull(store.read("1:owner", "owner", 0))
        }
    }

    @Test fun collectionPreferencesUseTheCurrentLibraryAccountAndExistingFactory() {
        val session = LibraryAccount()
        var created = 0
        val controller = UIViewController()
        val library = IosCollectionsLibrary(session, {}, {}, {}, {
            created++
            controller
        })
        assertNull(library.makePreferences())
        session.signedIn = true
        assertSame(controller, library.makePreferences())
        session.signedIn = false
        assertNull(library.makePreferences())
        assertEquals(1, created)
        library.close()
    }

    private fun withProfileGraph(platformModules: List<Module> = iosPlatformModules(BASE_URL), block: (Koin) -> Unit) {
        val app = koinApplication { modules(platformModules) }
        try { block(app.koin) } finally { app.close() }
    }

    private companion object { const val BASE_URL = "https://fixture.invalid" }
}

private class LibraryAccount : CollectionsLibrarySession {
    var signedIn = false
    override fun current() = CollectionsLibrarySnapshot(0, signedIn, false, false, false, "", emptyList())
    override fun observe(onState: (CollectionsLibrarySnapshot) -> Unit,
        onEvent: (CollectionsLibraryEvent) -> Unit) = CollectionsLibraryObservation {}
    override fun reload() = Unit
    override fun query(value: String) = Unit
    override fun openEditor(id: String?) = Unit
    override fun signIn() = Unit
    override fun back() = Unit
    override fun loadCover(id: String, accountVersion: Int, onCover: (ByteArray?, String?) -> Unit) = CollectionsLibraryObservation {}
    override fun close() = Unit
}
