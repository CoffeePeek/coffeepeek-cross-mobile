package com.coffeepeek.feature.homecheckin.impl.ui.data

/** Intentionally public composition ports, not API-module/domain re-exports or another backend. */
interface HomeCheckInEditorCatalog {
    suspend fun brewMethods(): Result<List<HomeCheckInBrewMethod>>
    suspend fun searchBeans(query: String): Result<List<HomeCheckInBeanOption>>
    /** The owner's bags on the shelf, in shelf order; none where the shelf is unavailable. */
    suspend fun shelfBags(): Result<List<HomeCheckInShelfBag>> = Result.success(emptyList())
}

data class HomeCheckInBrewMethod(val code: String, val name: String, val legacyCodes: Set<String> = emptySet())
data class HomeCheckInBeanOption(val slug: String, val name: String, val roasterName: String)
/** A bag to brew from; [name] is null for a bag added by its photo alone, [imageUrl] null for a bag without a card. */
data class HomeCheckInShelfBag(val id: String, val name: String?, val roasterName: String?, val imageUrl: String? = null)
