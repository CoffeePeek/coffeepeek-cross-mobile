package com.coffeepeek.feature.shelf.impl.ui

import com.coffeepeek.feature.shelf.api.ShelfBrew
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal fun testCalendar(now: String = "2026-10-27T12:00:00Z", zone: ShelfTimeZone = ShelfTimeZone.Utc) =
    ShelfCalendar(zone) { Instant.parse(now) }

internal fun bag(id: String, finished: Boolean = false, addedAt: String = "2026-10-20T08:00:00Z", finishedAt: String? = null) = ShelfBag(
    id = id, status = if (finished) ShelfBagStatus.Finished else ShelfBagStatus.OnShelf, position = null, photo = null,
    coffeeProductId = null, coffeeProductSlug = null, name = "Bag $id", roasterName = null, weightGrams = 250, grind = null,
    roastedOn = null, addedAtUtc = addedAt, finishedAtUtc = finishedAt ?: "2026-10-25T08:00:00Z".takeIf { finished },
    rating = null, buyAgain = null,
)

/** Mirrors the real repository's optimistic snapshot closely enough for the screens: writes change [shelfState] at once. */
internal class FakeShelfRepository(bags: List<ShelfBag>? = null) : ShelfRepository {
    val shelfState = MutableStateFlow(bags)
    val calls = mutableListOf<String>()
    var refreshResult: () -> Result<List<ShelfBag>> = { Result.success(shelfState.value.orEmpty()) }
    var writeFailure: ShelfIssue? = null
    var archivePages: (page: Int, query: String?) -> Result<ShelfArchivePage> = { page, _ -> Result.success(ShelfArchivePage(emptyList(), 0, page, 20)) }
    var gate: (suspend () -> Unit)? = null

    override val shelf = shelfState

    override suspend fun refresh(): Result<List<ShelfBag>> { calls += "refresh"; return refreshResult() }

    override suspend fun archive(page: Int, pageSize: Int, query: String?): Result<ShelfArchivePage> {
        calls += "archive:$page:${query.orEmpty()}"
        return archivePages(page, query)
    }

    val added = mutableListOf<ShelfBagDetails>()

    override suspend fun add(details: ShelfBagDetails, card: ShelfBagCard?): Result<ShelfBag> = write("add") {
        added += details
        bag("new").copy(coffeeProductSlug = (details.bean as? ShelfBean.Catalog)?.slug).also { shelfState.value = listOf(it) + shelfState.value.orEmpty() }
    }
    override suspend fun update(id: String, details: ShelfBagDetails, card: ShelfBagCard?): Result<ShelfBag> = error("Not used by these screens")

    override suspend fun move(id: String, toIndex: Int): Result<Unit> = write("move:$id:$toIndex") {
        val bags = shelfState.value.orEmpty()
        val moving = bags.first { it.id == id }
        shelfState.value = bags.filterNot { it.id == id }.toMutableList().apply { add(toIndex, moving) }
    }

    override suspend fun finish(id: String, rating: Int?, buyAgain: Boolean?): Result<ShelfBag> = write("finish:$id:$rating:$buyAgain") {
        shelfState.value = shelfState.value.orEmpty().filterNot { it.id == id }
        bag(id, finished = true)
    }

    override suspend fun restore(id: String, position: Int?): Result<ShelfBag> = write("restore:$id:$position") {
        val restored = bag(id)
        shelfState.value = shelfState.value?.toMutableList()?.apply { add((position ?: 0).coerceIn(0, size), restored) }
        restored
    }

    override suspend fun delete(id: String): Result<Unit> = write("delete:$id") {
        shelfState.value = shelfState.value?.filterNot { it.id == id }
    }

    private suspend fun <T> write(call: String, change: () -> T): Result<T> {
        calls += call
        gate?.invoke()
        writeFailure?.let { return Result.failure(ShelfFailure(it)) }
        return Result.success(change())
    }
}

/** Finishes the stages it is told to and returns [result]; [gate] holds the scan open. */
internal class FakeBagScanner(var readiness: BagScannerReadiness = BagScannerReadiness.Ready) : BagScanner {
    var result: Result<BagScanResult> = Result.success(scanResult(PackModerationStatus.Accepted))
    var prepareResult: Result<Unit> = Result.success(Unit)
    var gate: (suspend () -> Unit)? = null
    var scans = 0

    override suspend fun readiness() = readiness

    override suspend fun prepare(onProgress: (downloaded: Long, total: Long) -> Unit): Result<Unit> {
        onProgress(50, 100)
        onProgress(100, 100)
        return prepareResult.onSuccess { readiness = BagScannerReadiness.Ready }
    }

    override suspend fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): Result<BagScanResult> {
        scans++
        onStage(BagScanStage.Segmentation)
        gate?.invoke()
        BagScanStage.entries.drop(1).forEach(onStage)
        return result
    }
}

internal fun scanResult(status: PackModerationStatus, reasons: List<PackModerationReason> = emptyList(), card: Boolean = true) = BagScanResult(
    card = ShelfBagCard(byteArrayOf(1), "image/jpeg", "bag.jpg").takeIf { card }, cardWidth = 720, cardHeight = 960, frame = null,
    quality = null, moderation = PackModeration(status, reasons), timings = BagScanTimings(0, 0, 0, 0, 0),
)

/** Answers [result], records each date range asked for and, with a [gate], holds the answer until it completes. */
internal class FakeShelfBrewLog(var result: Result<List<ShelfBrew>> = Result.success(emptyList())) : ShelfBrewLog {
    val ranges = mutableListOf<Pair<String, String>>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun brews(from: String, to: String): Result<List<ShelfBrew>> {
        ranges += from to to
        gate?.await()
        return result
    }
}

internal fun brew(id: String, preparedOn: String, product: String? = null, bean: String? = null, dose: Double? = null, score: Int? = null) =
    ShelfBrew(id, preparedOn, product, bean, method = "Hario V60", doseGrams = dose, score = score)
