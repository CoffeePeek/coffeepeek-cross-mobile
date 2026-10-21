package com.coffeepeek.feature.homecheckin.impl.ui.data

/** Intentionally public composition ports, not API-module/domain re-exports or another backend. */
interface HomeCheckInEditorCatalog {
    suspend fun brewMethods(): Result<List<HomeCheckInBrewMethod>>
    suspend fun searchBeans(query: String): Result<List<HomeCheckInBeanOption>>
}

data class HomeCheckInBrewMethod(val code: String, val name: String, val legacyCodes: Set<String> = emptySet())
data class HomeCheckInBeanOption(val slug: String, val name: String, val roasterName: String)
