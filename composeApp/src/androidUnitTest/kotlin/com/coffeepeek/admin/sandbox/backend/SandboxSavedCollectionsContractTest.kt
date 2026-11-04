package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.collections.data.repository.createCollectionsRepository
import com.coffeepeek.feature.collections.data.repository.createSavedCollectionsRepository
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import io.ktor.client.request.get
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SandboxSavedCollectionsContractTest {
    private val dispatcher = StandardTestDispatcher()
    private class Fixture(dispatcher: CoroutineDispatcher) : java.io.Closeable {
        val backend = SandboxBackend(byteArrayOf(1, 2, 3), engineDispatcher = dispatcher)
        val owner = MutableStateFlow<String?>("sandbox")
        val account = object : CollectionsAccount {
            override val identities = owner
            override fun currentId() = owner.value
            override suspend fun loadId() = currentId()
        }
        val repository = createSavedCollectionsRepository(backend.gateway, backend.gateway, SandboxBackend.BASE_URL, account) { request, owner ->
            request.headers.append(SandboxSavedCollectionsBackend.OWNER, owner)
        }
        val snapshots = createCollectionsRepository(backend.gateway, backend.gateway, SandboxBackend.BASE_URL, account)
        suspend fun create(title: String = "List", visibility: SavedCollectionVisibility = SavedCollectionVisibility.Private) =
            repository.create(SavedCollectionMetadata(title, "Description", visibility)).getOrThrow()
        override fun close() = backend.close()
    }

    @Test fun privateListsRemainOwnerOnlyAndTheOldSnapshotRouteStillWorks() = runTest(dispatcher) {
        Fixture(dispatcher).use { fixture ->
            val row = fixture.create()
            assertEquals(0, row.summary.itemCount)
            assertTrue(fixture.repository.public(row.summary.id).isFailure)
            fixture.owner.value = "other"
            val otherLists = fixture.repository.mine().getOrThrow()
            assertEquals(listOf("Избранное"), otherLists.map { it.title })
            assertTrue(otherLists.all { it.visibility == SavedCollectionVisibility.Private })
            assertTrue(fixture.repository.owned(row.summary.id).isFailure)
            fixture.owner.value = "sandbox"
            assertEquals("Description", fixture.repository.owned(row.summary.id).getOrThrow().summary.description)
            val places = fixture.snapshots.favorites().getOrThrow()
            val snapshot = fixture.snapshots.create("Snapshot", places).getOrThrow()
            assertEquals(snapshot.places, fixture.snapshots.read(snapshot.id).getOrThrow().places)
            assertEquals(places, fixture.snapshots.favorites().getOrThrow())
        }
    }

    @Test fun metadataRevisionsRejectConflictsAndOnlyAPublicOwnedListCanBeFeatured() = runTest(dispatcher) {
        Fixture(dispatcher).use { fixture ->
            val row = fixture.create()
            assertTrue(fixture.repository.setFeatured(row.summary.id).isFailure)
            val public = SavedCollectionMetadata("Published", "Updated", SavedCollectionVisibility.Public)
            val changed = fixture.repository.update(row.summary.id, public, row.summary.revision).getOrThrow()
            assertTrue(fixture.repository.update(row.summary.id, public.copy(title = "Stale"), row.summary.revision).exceptionOrNull() is SavedCollectionConflict)
            fixture.repository.setFeatured(row.summary.id).getOrThrow()
            assertEquals("Published", fixture.repository.featured("sandbox").getOrThrow()?.summary?.title)
            fixture.repository.update(row.summary.id, public.copy(visibility = SavedCollectionVisibility.Private), changed.summary.revision).getOrThrow()
            assertNull(fixture.repository.featuredId().getOrThrow())
            assertNull(fixture.repository.featured("sandbox").getOrThrow())
            assertTrue(fixture.repository.public(row.summary.id).isFailure)
        }
    }

    @Test fun publishingAndRenamingTheDefaultNeverCreatesAPrivateCopy() = runTest(dispatcher) {
        Fixture(dispatcher).use { fixture ->
            val original = fixture.repository.mine().getOrThrow().single()
            assertTrue(original.isDefault)
            fixture.repository.update(original.id, SavedCollectionMetadata("Public weekend",
                visibility = SavedCollectionVisibility.Public), original.revision).getOrThrow()
            val after = fixture.repository.mine().getOrThrow().single()
            assertEquals(original.id, after.id)
            assertTrue(after.isDefault)
            assertEquals(SavedCollectionVisibility.Public, after.visibility)
        }
    }

    @Test fun membershipUpdatesBothKindsAtomicallyWithoutChangingGlobalFavorites() = runTest(dispatcher) {
        Fixture(dispatcher).use { fixture ->
            val first = fixture.create("Private")
            val second = fixture.create("Public", SavedCollectionVisibility.Public)
            val places = fixture.snapshots.favorites().getOrThrow()
            val expected = fixture.snapshots.create("Public card reference", places).getOrThrow().places
            for (place in places) {
                val target = SavedCollectionTarget(place.kind, place.slug)
                fixture.repository.memberships(target, setOf(first.summary.id, second.summary.id)).getOrThrow()
                val memberships = fixture.repository.mine(target).getOrThrow()
                assertTrue(memberships.filter { it.id == first.summary.id || it.id == second.summary.id }.all { it.containsItem })
                assertFalse(memberships.single { it.title == "Избранное" }.containsItem)
            }
            val before = fixture.repository.mine().getOrThrow()
            val target = SavedCollectionTarget(places.first().kind, places.first().slug)
            assertTrue(fixture.repository.memberships(target, setOf(first.summary.id, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).isFailure)
            assertEquals(before, fixture.repository.mine().getOrThrow())
            assertEquals(places, fixture.snapshots.favorites().getOrThrow())
            assertEquals(expected, fixture.repository.owned(first.summary.id).getOrThrow().places)
        }
    }

    @Test fun coverUploadUsesLocalPresignedPutAndPrivateReadsNeverUseThePublicRoute() = runTest(dispatcher) {
        Fixture(dispatcher).use { fixture ->
            val input = CollectionCoverInput(byteArrayOf(7, 8, 9), "image/jpeg", "cover.jpg")
            val upload = fixture.repository.uploadCover(input).getOrThrow()
            val private = fixture.repository.create(SavedCollectionMetadata("Covered", coverUpload = upload)).getOrThrow()
            val cover = assertNotNull(private.summary.cover)
            assertContentEquals(input.bytes, fixture.repository.cover(private.summary.id, cover).getOrThrow())
            val publicPath = "${SandboxBackend.BASE_URL}/api/v1/favorite-collections/${private.summary.id}/cover/${cover.id}"
            assertEquals(HttpStatusCode.NotFound, fixture.backend.gateway.get(publicPath) { expectSuccess = false }.status)
            fixture.repository.update(private.summary.id, SavedCollectionMetadata("Covered", visibility = SavedCollectionVisibility.Public), private.summary.revision).getOrThrow()
            val public = fixture.repository.public(private.summary.id).getOrThrow()
            assertContentEquals(input.bytes, fixture.repository.cover(public.summary.id, assertNotNull(public.summary.cover)).getOrThrow())
        }
    }

    @Test fun anotherOwnerCannotAttachAnUploadedCoverAndUploadFailuresDoNotCreateAList() = runTest(dispatcher) {
        Fixture(dispatcher).use { fixture ->
            val input = CollectionCoverInput(byteArrayOf(7, 8, 9), "image/jpeg", "cover.jpg")
            val upload = fixture.repository.uploadCover(input).getOrThrow()
            fixture.owner.value = "other"
            assertTrue(fixture.repository.create(SavedCollectionMetadata("Wrong owner", coverUpload = upload)).isFailure)
            fixture.backend.scenario = SandboxScenario.UploadError
            assertTrue(fixture.repository.uploadCover(input).isFailure)
            assertEquals(listOf("Избранное"), fixture.repository.mine().getOrThrow().map { it.title })
        }
    }
}
