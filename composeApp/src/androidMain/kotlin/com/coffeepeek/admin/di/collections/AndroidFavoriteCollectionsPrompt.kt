package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind

internal class AndroidFavoriteCollectionsPrompt(private val requests: CollectionPickerRequests) : FavoriteCollectionsPrompt {
    override fun shop(slug: String) = request(CollectionTarget(CollectionTargetKind.CoffeeShop, slug))
    override fun roaster(slug: String) = request(CollectionTarget(CollectionTargetKind.Roaster, slug))

    private fun request(target: CollectionTarget) {
        if (target.slug.isBlank()) return
        if (!requests.request(target)) Navigator.navigate(Navigator.Screen.Auth)
    }
}
