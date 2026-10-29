package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationShelfBagReader
import com.coffeepeek.feature.shelf.api.ShelfBrew
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.data.repository.ShelfAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val PAGE_SIZE = 100
private const val MAX_PAGES = 5

/**
 * The home check-ins that name a bag, for its card: every page, up to [MAX_PAGES]. Read for the shelf's owner only; an answer
 * that arrives after the account changed is dropped.
 */
internal class ShelfHomeBrews(private val reader: HomePreparationShelfBagReader, private val account: ShelfAccount) : ShelfBrewLog {
    override suspend fun brews(bagId: String): Result<List<ShelfBrew>> = try {
        val owner = checkNotNull(account.owner) { "No shelf owner" }
        val brews = mutableListOf<ShelfBrew>()
        for (page in 1..MAX_PAGES) {
            val result = reader.forShelfBag(bagId, page, PAGE_SIZE).getOrThrow()
            result.items.mapTo(brews) { it.shelfBrew() }
            if (!result.hasMore) break
        }
        currentCoroutineContext().ensureActive()
        check(account.owner == owner) { "The shelf owner changed" }
        Result.success(brews)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        Result.failure(error)
    }
}

private fun HomePreparation.shelfBrew() = ShelfBrew(
    id = id, preparedOn = preparedOn, method = brewMethodCode?.methodLabel(),
    doseGrams = doseGrams?.replace(',', '.')?.toDoubleOrNull(), score = tasteScore,
)
