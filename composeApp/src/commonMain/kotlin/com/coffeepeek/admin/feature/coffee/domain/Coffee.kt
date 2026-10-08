package com.coffeepeek.admin.feature.coffee.domain

internal data class Coffee(
    val slug: String,
    val name: String,
    val photoUrl: String?,
    val roasterName: String,
    val roasterPhotoUrl: String?,
    val countries: List<String>,
    val tasteCodes: List<String>,
    val offers: List<CoffeeOffer>,
)

internal data class CoffeeOffer(
    val price: Double?,
    val currency: String?,
    val weightGrams: Int?,
    val key: String? = null,
    val availability: CoffeeAvailability = CoffeeAvailability.Unknown,
    val sellerName: String? = null,
    val sourceUrl: String? = null,
    val grind: String? = null,
    val brewPurpose: String? = null,
    val availabilityScope: String? = null,
    val checkedAtUtc: String? = null,
)

internal enum class CoffeeAvailability { InStock, OutOfStock, PreOrder, Unknown }

internal data class CoffeeDetails(
    val coffee: Coffee,
    val canonicalPath: String,
    val description: String?,
    val photos: List<String>,
    val roasterSlug: String?,
    val productKind: String?,
    val productForm: String?,
    val roastLevel: String?,
    val acidity: String?,
    val tasteDescriptors: List<String>,
    val checkedAtUtc: String?,
)

internal data class CoffeePage(val items: List<Coffee>, val currentPage: Int, val totalPages: Int)

internal data class CoffeeFilterOption(val code: String, val name: String)

internal data class CoffeeFilterGroup(val code: String, val name: String, val options: List<CoffeeFilterOption>)

internal data class CoffeeFilters(
    val values: Map<String, Set<String>> = emptyMap(),
    val availableOnly: Boolean = false,
) {
    val activeCount: Int get() = values.values.sumOf { it.size } + if (availableOnly) 1 else 0

    fun toggle(group: String, code: String): CoffeeFilters {
        val selected = values[group].orEmpty()
        val next = if (code in selected) selected - code else selected + code
        return copy(values = if (next.isEmpty()) values - group else values + (group to next))
    }
}

internal interface CoffeeRepository {
    suspend fun search(query: String, filters: CoffeeFilters, page: Int): Result<CoffeePage>
    suspend fun getFilterGroups(): Result<List<CoffeeFilterGroup>>
    suspend fun getDetails(slug: String): Result<CoffeeDetails>
}
