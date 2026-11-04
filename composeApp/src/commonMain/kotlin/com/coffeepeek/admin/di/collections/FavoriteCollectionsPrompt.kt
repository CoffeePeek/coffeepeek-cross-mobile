package com.coffeepeek.admin.di.collections

/** App-level heart action. Android supplies the collections picker; other platforms retain their current writer. */
interface FavoriteCollectionsPrompt {
    fun shop(slug: String)
    fun roaster(slug: String)
}
