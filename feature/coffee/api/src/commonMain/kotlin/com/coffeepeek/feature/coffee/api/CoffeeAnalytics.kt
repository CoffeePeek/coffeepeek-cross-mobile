package com.coffeepeek.feature.coffee.api

/**
 * Narrow telemetry ports. The application adapts them to its consent-aware analytics; no query text,
 * slugs, names or SDK types cross this boundary. [None] keeps previews and isolated tests silent.
 */
interface CoffeeAnalytics {
    /** One tracker per list ViewModel: it owns the active search attempt. */
    fun list(): CoffeeListAnalytics
    fun detail(): CoffeeDetailAnalytics

    companion object {
        val None: CoffeeAnalytics = object : CoffeeAnalytics {
            override fun list() = CoffeeListAnalytics.None
            override fun detail() = CoffeeDetailAnalytics.None
        }
    }
}

interface CoffeeListAnalytics {
    /**
     * A new reset search. [fingerprint] is canonical for the normalized query plus filters, so a refresh with
     * unchanged criteria does not start a second attempt. [filterKeys] lists only groups with selections.
     */
    fun searchStarted(fingerprint: String, query: String, filterKeys: List<String>)
    fun resultsViewed(count: Int)
    fun searchFailed(error: Throwable)
    /** 1-based position of the opened result. */
    fun opened(position: Int)
    fun filtersOpened()
    fun filtersApplied(filterKeys: List<String>, changedKeys: List<String>, reset: Boolean)

    companion object {
        val None: CoffeeListAnalytics = object : CoffeeListAnalytics {
            override fun searchStarted(fingerprint: String, query: String, filterKeys: List<String>) = Unit
            override fun resultsViewed(count: Int) = Unit
            override fun searchFailed(error: Throwable) = Unit
            override fun opened(position: Int) = Unit
            override fun filtersOpened() = Unit
            override fun filtersApplied(filterKeys: List<String>, changedKeys: List<String>, reset: Boolean) = Unit
        }
    }
}

enum class CoffeeDetailSection { Photos, Description, Roaster, SimilarCoffees, Contacts }
enum class CoffeeContentAction { Open, Close, Select, Share }

interface CoffeeDetailAnalytics {
    fun contentInteracted(section: CoffeeDetailSection, action: CoffeeContentAction)
    /** Navigating to the roaster / a similar coffee; lets the destination attribute its entry point. */
    fun roasterOpened()
    fun similarOpened()

    companion object {
        val None: CoffeeDetailAnalytics = object : CoffeeDetailAnalytics {
            override fun contentInteracted(section: CoffeeDetailSection, action: CoffeeContentAction) = Unit
            override fun roasterOpened() = Unit
            override fun similarOpened() = Unit
        }
    }
}

/** Sections whose on-screen exposure the app measures; the feature only marks the container. */
enum class CoffeeExposure { Catalog, Photos, Roaster, Offers, SimilarCoffees, Search, Filters }
