package com.coffeepeek.feature.shelf.domain.repository

import com.coffeepeek.feature.shelf.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * The signed-in user's shelf. [shelf] is an in-memory snapshot of the current account: null until loaded, cleared when the account
 * changes. [move] and [finish] update it at once and roll back when the server refuses; every failure is a [ShelfFailure].
 */
interface ShelfRepository {
    val shelf: Flow<List<ShelfBag>?>

    suspend fun refresh(): Result<List<ShelfBag>>
    suspend fun archive(page: Int, pageSize: Int, query: String? = null): Result<ShelfArchivePage>

    /** Uploads [card] (when present) and puts the new bag first on the shelf. */
    suspend fun add(details: ShelfBagDetails, card: ShelfBagCard?): Result<ShelfBag>
    /** Replaces the details; a [card] replaces the image ("Переснять"), null keeps it. */
    suspend fun update(id: String, details: ShelfBagDetails, card: ShelfBagCard?): Result<ShelfBag>

    suspend fun move(id: String, toIndex: Int): Result<Unit>
    suspend fun finish(id: String, rating: Int?, buyAgain: Boolean?): Result<ShelfBag>
    /** Returns an archived bag to [position] (first when null); also the undo of [finish]. */
    suspend fun restore(id: String, position: Int? = null): Result<ShelfBag>
    suspend fun delete(id: String): Result<Unit>
}
