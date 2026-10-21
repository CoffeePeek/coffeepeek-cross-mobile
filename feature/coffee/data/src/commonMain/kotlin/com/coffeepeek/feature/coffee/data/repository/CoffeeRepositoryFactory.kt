package com.coffeepeek.feature.coffee.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.coffee.data.backend.CoffeeBackend
import com.coffeepeek.feature.coffee.data.backend.CoffeeSearchRequestDto
import com.coffeepeek.feature.coffee.data.mapper.toDetails
import com.coffeepeek.feature.coffee.data.mapper.toDomain
import com.coffeepeek.feature.coffee.data.mapper.toJson
import com.coffeepeek.feature.coffee.domain.model.CoffeeDetails
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterGroup
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.model.CoffeePage
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import io.ktor.client.HttpClient

/** The application owns/configures the plain catalogue client and its lifetime. */
fun createCoffeeRepository(client: HttpClient): CoffeeRepository = RemoteCoffeeRepository(CoffeeBackend(client))

private class RemoteCoffeeRepository(private val backend: CoffeeBackend) : CoffeeRepository {
    override suspend fun getDetails(slug: String): Result<CoffeeDetails> = requestResult {
        backend.detail(slug).toDetails()
    }

    override suspend fun search(query: String, filters: CoffeeFilters, page: Int): Result<CoffeePage> = requestResult {
        require(query.length <= 100 && page in 1..100_000)
        val data = backend.search(CoffeeSearchRequestDto(query.trim(), filters.toJson(), page))
        CoffeePage(data.items.map { it.toDomain() }, data.currentPage, data.totalPages, data.totalCount)
    }

    override suspend fun getFilterGroups(): Result<List<CoffeeFilterGroup>> = requestResult {
        backend.filters().toDomain()
    }
}
