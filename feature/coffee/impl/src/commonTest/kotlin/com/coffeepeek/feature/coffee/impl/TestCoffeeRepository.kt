package com.coffeepeek.feature.coffee.impl

import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository

internal data class SearchCall(val query: String, val filters: CoffeeFilters, val page: Int)

internal class TestCoffeeRepository : CoffeeRepository {
    val searches = mutableListOf<SearchCall>()
    val details = mutableListOf<String>()
    var filterCalls = 0
    var search: suspend (SearchCall) -> Result<CoffeePage> = { Result.success(page()) }
    var detail: suspend (String) -> Result<CoffeeDetails> = { Result.success(detail()) }
    var filters: suspend () -> Result<List<CoffeeFilterGroup>> = { Result.success(groups()) }

    override suspend fun search(query: String, filters: CoffeeFilters, page: Int): Result<CoffeePage> {
        val call = SearchCall(query, filters, page)
        searches += call
        return search(call)
    }
    override suspend fun getDetails(slug: String): Result<CoffeeDetails> {
        details += slug
        return detail(slug)
    }
    override suspend fun getFilterGroups(): Result<List<CoffeeFilterGroup>> {
        filterCalls++
        return filters()
    }
}

internal fun coffee(slug: String = "coffee", tastes: List<String> = emptyList(), offers: List<CoffeeOffer> = emptyList()) = Coffee(
    slug = slug, name = "Coffee $slug", photoUrl = null, roasterName = "Roast", roasterPhotoUrl = null,
    countries = emptyList(), tasteCodes = tastes, offers = offers,
)

internal fun page(vararg slugs: String = arrayOf("coffee"), current: Int = 1, total: Int = 1) =
    CoffeePage(slugs.map { coffee(it) }, current, total)

internal fun detail(
    coffee: Coffee = coffee(),
    photos: List<String> = listOf("photo"),
    description: String? = "Description",
    roaster: String? = "roast",
) = CoffeeDetails(coffee = coffee, canonicalPath = "/coffees/${coffee.slug}", description = description,
    photos = photos, roasterSlug = roaster, productKind = null, productForm = null,
    roastLevel = null, acidity = null, tasteDescriptors = emptyList(), checkedAtUtc = null)

internal fun groups() = listOf(CoffeeFilterGroup("taste", "Taste", listOf(CoffeeFilterOption("fruit", "Fruit"))))
