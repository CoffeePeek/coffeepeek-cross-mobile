package com.coffeepeek.feature.coffee.domain.repository

import com.coffeepeek.feature.coffee.domain.model.CoffeeDetails
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterGroup
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.model.CoffeePage

interface CoffeeRepository {
    suspend fun search(query: String, filters: CoffeeFilters, page: Int): Result<CoffeePage>
    suspend fun getFilterGroups(): Result<List<CoffeeFilterGroup>>
    suspend fun getDetails(slug: String): Result<CoffeeDetails>
}
