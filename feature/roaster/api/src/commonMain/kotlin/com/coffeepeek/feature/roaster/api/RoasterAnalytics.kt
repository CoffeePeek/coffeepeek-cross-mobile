package com.coffeepeek.feature.roaster.api

/**
 * Narrow telemetry ports. The application adapts them to its consent-aware analytics; no query text, slugs, names
 * or SDK types cross this boundary. [None] keeps previews and isolated tests silent.
 */
interface RoasterAnalytics {
    /** One tracker per list ViewModel: it owns the active search attempt. */
    fun list(): RoasterListAnalytics
    fun detail(): RoasterDetailAnalytics

    companion object {
        val None: RoasterAnalytics = object : RoasterAnalytics {
            override fun list() = RoasterListAnalytics.None
            override fun detail() = RoasterDetailAnalytics.None
        }
    }
}

interface RoasterListAnalytics {
    /**
     * The visible result set after loading/filtering settled. [fingerprint] is canonical for the criteria, so
     * re-reporting the same criteria is a no-op for the adapter. [filterKeys] names only active filter kinds.
     * [resultCount] is meaningful when [failed] is false.
     */
    fun resultsShown(fingerprint: String, query: String, filterKeys: List<String>, resultCount: Int, failed: Boolean)
    /** 1-based position among the visible results. */
    fun opened(position: Int)

    companion object {
        val None: RoasterListAnalytics = object : RoasterListAnalytics {
            override fun resultsShown(fingerprint: String, query: String, filterKeys: List<String>, resultCount: Int, failed: Boolean) = Unit
            override fun opened(position: Int) = Unit
        }
    }
}

enum class RoasterDetailSection { Photos, Shops, Contacts }
enum class RoasterContentAction { Open, Select, Instagram, Website }

interface RoasterDetailAnalytics {
    fun contentInteracted(section: RoasterDetailSection, action: RoasterContentAction)
    /** Navigating to one of the roaster's shops; lets the destination attribute its entry point. */
    fun shopOpened()

    companion object {
        val None: RoasterDetailAnalytics = object : RoasterDetailAnalytics {
            override fun contentInteracted(section: RoasterDetailSection, action: RoasterContentAction) = Unit
            override fun shopOpened() = Unit
        }
    }
}

/** Sections whose on-screen exposure the app measures; the feature only marks the container. */
enum class RoasterExposure { Catalog, Discovery, Photos, Contacts, Shops, FavoriteCard, FavoriteDetail }
