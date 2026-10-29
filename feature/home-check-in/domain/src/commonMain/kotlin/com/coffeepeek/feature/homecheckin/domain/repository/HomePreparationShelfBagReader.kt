package com.coffeepeek.feature.homecheckin.domain.repository

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPage

/** The signed-in owner's preparations brewed from one shelf bag, newest first. Account lifetime is supplied by composition. */
fun interface HomePreparationShelfBagReader {
    suspend fun forShelfBag(shelfBagId: String, page: Int, pageSize: Int): Result<HomePreparationPage>
}
