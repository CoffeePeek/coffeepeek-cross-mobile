package com.coffeepeek.admin.di.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.feature.favorites.api.FavoritesScreenAnalytics

internal object AppFavoritesScreenAnalytics : FavoritesScreenAnalytics {
    @Composable override fun heart(modifier: Modifier, roaster: Boolean): Modifier {
        val page = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
        val placement = when (page) {
            "shop_detail" -> "shop_detail"
            "roaster_detail" -> "roaster_card"
            else -> "catalog_card"
        }
        return modifier.analyticsExposure("favorites", placement, page)
    }
}
