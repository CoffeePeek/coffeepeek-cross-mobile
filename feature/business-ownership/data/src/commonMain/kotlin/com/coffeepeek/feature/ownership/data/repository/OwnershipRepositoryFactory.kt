package com.coffeepeek.feature.ownership.data.repository

import com.coffeepeek.feature.ownership.data.backend.*
import com.coffeepeek.feature.ownership.data.mapper.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException

fun createBusinessOwnershipRepository(client: HttpClient, uploader: BusinessPhotoUploader, account: OwnershipAccount): BusinessOwnershipRepository =
    OwnershipRepository(OwnershipBackend(client), uploader, account)

private class OwnershipRepository(private val backend: OwnershipBackend, private val uploader: BusinessPhotoUploader, private val account: OwnershipAccount) : BusinessOwnershipRepository {
    override suspend fun claims(page: Int) = scoped { backend.claims(page).domain { it.domain() } }
    override suspend fun claim(id: String) = scoped { backend.claim(id).domain() }
    override suspend fun submit(input: ClaimSubmission) = scoped { backend.submit(input.validated()).domain() }
    override suspend fun businesses(kind: BusinessKind, page: Int) = scoped {
        if (kind == BusinessKind.Roaster) backend.roasters(page).domain { OwnedBusiness(it.id, kind, it.name) }
        else backend.shops().items.map { OwnedBusiness(it.id, kind, it.name) }.let { BusinessPage(it, it.size, 1, maxOf(1, it.size)) }
    }
    override suspend fun profile(kind: BusinessKind, id: String) = scoped { backend.profile(kind, id).domain(kind) }
    override suspend fun save(original: BusinessProfile, draft: ProfileDraft) = scoped { owner ->
        val edited = draft.applyTo(original)
        val fresh = draft.photos.filter { it.bytes != null }
        val uploads = if (fresh.isEmpty()) emptyList() else uploader.upload(original.kind, fresh).getOrThrow()
        checkAccount(owner)
        check(uploads.size == fresh.size)
        val replacements = fresh.zip(uploads).associate { it.first.key to it.second }
        val photos = draft.photos.map { replacements[it.key] ?: it }
        backend.write(edited, if (draft.photos == original.photos) null else photos)
        checkAccount(owner)
        finishSave(original, photos, owner)
    }
    private suspend fun finishSave(original: BusinessProfile, photos: List<BusinessPhoto>, owner: String): BusinessProfile = try {
        if (original.kind == BusinessKind.CoffeeShop) saveShopGallery(original, photos, owner)
        checkAccount(owner)
        backend.profile(original.kind, original.id).domain(original.kind)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: OwnershipFailure) {
        if (failure.problem == OwnershipProblem.SignedOut) throw failure
        throw OwnershipFailure(OwnershipProblem.PartialSave)
    }
    catch (_: Exception) { throw OwnershipFailure(OwnershipProblem.PartialSave) }

    private suspend fun saveShopGallery(original: BusinessProfile, photos: List<BusinessPhoto>, owner: String) {
        if (photos == original.photos) return
        checkAccount(owner)
        val originalKeys = original.photos.map { it.storageKey }.toSet()
        val added = photos.filter { it.storageKey !in originalKeys }
        val attached = if (added.isEmpty()) original else backend.addPhotos(original.id, added).domain(original.kind)
        checkAccount(owner)
        val kept = photos.map { it.storageKey }.toSet()
        val removedIds = original.photos.filter { it.storageKey !in kept }.map { checkNotNull(it.id) }
        if (removedIds.isNotEmpty()) backend.removePhotos(original.id, removedIds)
        checkAccount(owner)
        val idsByKey = attached.photos.associate { it.storageKey to checkNotNull(it.id) }
        backend.orderPhotos(original.id, photos.map { checkNotNull(idsByKey[it.storageKey]) })
    }
    private fun checkAccount(owner: String) {
        if (account.current() != owner) throw OwnershipFailure(OwnershipProblem.SignedOut)
    }
    private suspend fun <T> scoped(block: suspend (String) -> T): Result<T> = result {
        val owner = account.current() ?: throw OwnershipFailure(OwnershipProblem.SignedOut)
        val value = block(owner)
        checkAccount(owner)
        value
    }
}
private suspend fun <T> result(block: suspend () -> T): Result<T> = requestResult {
    try { block() } catch (failure: ResponseException) { throw problem(failure.response.status.value) }
}
