package com.coffeepeek.feature.homecheckin.domain.repository

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationInput

/** Account lifetime/revocation is supplied by application composition. No implicit write retries. */
interface HomePreparationRepository : HomePreparationReader {
    suspend fun detail(id: String): Result<HomePreparation>
    suspend fun create(input: HomePreparationInput): Result<HomePreparation>
    suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation>
    suspend fun delete(id: String): Result<Unit>
}
