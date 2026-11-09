package com.coffeepeek.feature.shop.data.local

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.SavedShopCheckInDraft
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInDraftStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import kotlin.time.Clock

fun createShopCheckInDraftStorage(rootPath: String): ShopCheckInDraftStorage =
    FileShopCheckInDraftStorage(rootPath.toPath(), draftFileSystem)

internal expect val draftFileSystem: FileSystem

internal class FileShopCheckInDraftStorage(
    private val root: Path,
    private val files: FileSystem,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ShopCheckInDraftStorage {
    private val changes = MutableStateFlow(0L)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(accountId: String, shopId: String): SavedShopCheckInDraft? = withContext(Dispatchers.IO) {
        val folder = shopFolder(accountId, shopId)
        val manifest = readManifest(folder) ?: return@withContext null
        check(manifest.shopId == shopId)
        val photos = manifest.photos.map { photo ->
            check(photo.digest.length == 64 && photo.digest.all { it in "0123456789abcdef" })
            ShopCheckInPhoto(files.read(folder / photo.digest) { readByteArray() }, photo.fileName, photo.contentType)
        }
        SavedShopCheckInDraft(shopId, manifest.input(photos), manifest.deliveryUnconfirmed)
    }

    override suspend fun save(accountId: String, draft: SavedShopCheckInDraft) = withContext(Dispatchers.IO) {
        val folder = shopFolder(accountId, draft.shopId)
        files.createDirectories(folder)
        val photos = draft.input.photos.map { storePhoto(folder, it) }
        val manifest = DraftManifest.from(draft, photos, now())
        files.write(folder / MANIFEST_TEMP) { writeUtf8(json.encodeToString(manifest)) }
        // Until this rename, the previous manifest and all of its photos are still valid.
        files.atomicMove(folder / MANIFEST_TEMP, folder / MANIFEST)
        changes.update { it + 1 }
        removeUnusedPhotos(folder, photos.mapTo(mutableSetOf()) { it.digest })
    }

    override suspend fun delete(accountId: String, shopId: String) = withContext(Dispatchers.IO) {
        val folder = shopFolder(accountId, shopId)
        files.delete(folder / MANIFEST)
        changes.update { it + 1 }
        // A cleanup failure after unlinking the manifest cannot restore a submitted draft.
        try { files.deleteRecursively(folder) } catch (_: IOException) { }
    }

    override fun observeLatestShop(accountId: String) = changes.map {
        withContext(Dispatchers.IO) {
            files.listOrNull(accountFolder(accountId)).orEmpty()
                .mapNotNull(::readManifestIfAvailable)
                .maxByOrNull { it.updatedAt }
                ?.slug
        }
    }.distinctUntilChanged()

    private fun storePhoto(folder: Path, photo: ShopCheckInPhoto): DraftPhoto {
        val digest = photo.bytes.toByteString().sha256().hex()
        val path = folder / digest
        if (!files.exists(path)) {
            val temporary = folder / PHOTO_TEMP
            files.write(temporary) { write(photo.bytes) }
            files.atomicMove(temporary, path)
        }
        return DraftPhoto(digest, photo.fileName, photo.contentType)
    }

    private fun readManifest(folder: Path): DraftManifest? {
        val path = folder / MANIFEST
        return if (files.exists(path)) files.read(path) { json.decodeFromString<DraftManifest>(readUtf8()) } else null
    }

    private fun readManifestIfAvailable(folder: Path): DraftManifest? = try {
        readManifest(folder)
    } catch (_: IOException) {
        null
    } catch (_: SerializationException) {
        null
    }

    private fun removeUnusedPhotos(folder: Path, keep: Set<String>) {
        try {
            files.list(folder).filter { it.name != MANIFEST && it.name !in keep }
                .forEach { files.delete(it) }
        } catch (_: IOException) {
            // The new manifest is committed. Any leftovers can be removed on the next save.
        }
    }

    private fun accountFolder(accountId: String) = root / accountId.encodeUtf8().sha256().hex()
    private fun shopFolder(accountId: String, shopId: String) =
        accountFolder(accountId) / shopId.encodeUtf8().sha256().hex()
}

private const val MANIFEST = "draft.json"
private const val MANIFEST_TEMP = "draft.tmp"
private const val PHOTO_TEMP = "photo.tmp"

@Serializable
private data class DraftPhoto(val digest: String, val fileName: String, val contentType: String)

@Serializable
private data class DraftManifest(
    val shopId: String,
    val slug: String,
    val text: String,
    val coffee: Int,
    val service: Int,
    val place: Int,
    val visitedAt: String,
    val visibility: String,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<DraftPhoto>,
    val deliveryUnconfirmed: Boolean,
    val updatedAt: Long,
) {
    fun input(photos: List<ShopCheckInPhoto>) = ShopCheckInCreateInput(
        slug, text, ShopRating(place = place, service = service, coffee = coffee), visitedAt,
        ShopCheckInVisibility.valueOf(visibility), drinkSlug, customDrinkName, photos,
    )

    companion object {
        fun from(draft: SavedShopCheckInDraft, photos: List<DraftPhoto>, updatedAt: Long): DraftManifest {
            val input = draft.input
            return DraftManifest(draft.shopId, input.shopSlug, input.text, input.rating.coffee,
                input.rating.service, input.rating.place, input.visitedAtIso, input.visibility.name,
                input.drinkSlug, input.customDrinkName, photos, draft.deliveryUnconfirmed, updatedAt)
        }
    }
}
