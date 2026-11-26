package com.coffeepeek.admin.di.collections

/** App-level shop and roaster heart action. Both platforms select memberships before writing. */
interface FavoriteCollectionsPrompt {
    fun shop(slug: String)
    fun roaster(slug: String)
}
