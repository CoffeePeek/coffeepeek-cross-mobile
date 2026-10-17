package com.coffeepeek.feature.homecheckin.impl.ui.data

/** Presentation ports; composition will adapt the existing catalogue, not create another backend. */
internal interface HomeCheckInEditorCatalog {
    suspend fun brewMethods(): Result<List<HomeCheckInBrewMethod>>
    suspend fun searchBeans(query: String): Result<List<HomeCheckInBeanOption>>
}

internal data class HomeCheckInBrewMethod(val code: String, val name: String)
internal data class HomeCheckInBeanOption(val slug: String, val name: String, val roasterName: String)
