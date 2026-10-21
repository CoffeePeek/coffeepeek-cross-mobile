package com.coffeepeek.feature.roaster.domain.model

/** Public identity of a roaster; the slug is the route and favorite key. */
data class RoasterAddress(val slug: String, val canonicalPath: String, val revision: Int, val isAlias: Boolean)
