package com.coffeepeek.feature.discovery.impl.search

internal enum class SearchEmptyReason { Catalog, Query, Filters }

/** A successful, finished shop search only. Errors, pending work and partial pages are not empty results. */
internal val SearchState.emptyReason: SearchEmptyReason?
    get() = when {
        isLoading || isRefreshing || isLoadingMore || hasMore || error != null || loadMoreError != null -> null
        visibleShops.isNotEmpty() -> null
        activeFilterCount > 0 -> SearchEmptyReason.Filters
        query.isNotBlank() -> SearchEmptyReason.Query
        else -> SearchEmptyReason.Catalog
    }

internal val SearchState.canSuggestShop: Boolean
    get() = !isLoading && !isRefreshing && !isLoadingMore && !hasMore && error == null && loadMoreError == null

/** An unknown roaster state can be loading or failed; keep that section visible instead of claiming no matches. */
internal fun SearchState.hasNoSearchMatches(hasRoasterResults: Boolean?): Boolean =
    query.isNotBlank() && emptyReason != null && hasRoasterResults == false
