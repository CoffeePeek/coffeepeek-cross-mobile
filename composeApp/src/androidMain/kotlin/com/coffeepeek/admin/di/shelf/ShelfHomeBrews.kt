package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDateRange
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import com.coffeepeek.feature.shelf.api.ShelfBrew
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.data.repository.ShelfAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val PAGE_SIZE = 100
private const val MAX_PAGES = 5

/**
 * Home check-ins for a bag's card: every page of the date range, up to [MAX_PAGES]. Read for the shelf's owner only; an answer
 * that arrives after the account changed is dropped.
 */
internal class ShelfHomeBrews(private val reader: HomePreparationReader, private val account: ShelfAccount) : ShelfBrewLog {
    override suspend fun brews(from: String, to: String): Result<List<ShelfBrew>> = try {
        val owner = checkNotNull(account.owner) { "No shelf owner" }
        val range = HomePreparationDateRange(from, to)
        val brews = mutableListOf<ShelfBrew>()
        for (page in 1..MAX_PAGES) {
            val result = reader.page(page, PAGE_SIZE, range).getOrThrow()
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
    id = id, preparedOn = preparedOn, coffeeProductId = coffeeProductId, beanName = beanName,
    method = brewMethodCode?.methodLabel(), doseGrams = doseGrams?.replace(',', '.')?.toDoubleOrNull(), score = tasteScore,
)
