package com.coffeepeek.feature.homecheckin.domain.repository

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDateRange
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDay
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPage

/** Read-only boundary; account lifetime is supplied by application composition. */
interface HomePreparationReader {
    /** Null range reads all dates. Implementations must not silently clamp page arguments. */
    suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage>
    suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>>
}
