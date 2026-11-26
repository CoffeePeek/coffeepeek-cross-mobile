@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.designsystem.component.coffeePeekNativeDrawableImage
import com.coffeepeek.core.designsystem.component.coffeePeekNativeVectorImage
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.impl.resources.*
import com.coffeepeek.feature.shop.impl.ui.compose.component.shopBrewArtwork
import com.coffeepeek.feature.shop.impl.ui.compose.component.shopTagArtwork
import com.coffeepeek.feature.shop.impl.ui.compose.component.shortDayName
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopScheduleStatus
import com.coffeepeek.feature.shop.impl.ui.data.formatShopScheduleTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.*
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

internal class ShopDetailIosResources(val language: String) {
    private val environment = getSystemResourceEnvironment()
    private val scale = UIScreen.mainScreen.scale.toFloat()
    private val images = mutableMapOf<ShopFeature, UIImage>()
    suspend fun text(resource: StringResource, vararg args: Any) =
        if (args.isEmpty()) getString(environment, resource) else getString(environment, resource, *args)

    suspend fun feature(feature: ShopFeature): ShopDetailIosFeature {
        val image = images[feature] ?: run {
            withContext(Dispatchers.Default) {
                if (feature.isBrewMethod) coffeePeekNativeDrawableImage(
                    getDrawableResourceBytes(environment, shopBrewArtwork(feature.name)), 28f, scale)
                else coffeePeekNativeVectorImage(shopTagArtwork(feature.slug), 28f, scale)
            }.also { images[feature] = it }
        }
        return ShopDetailIosFeature(feature.name, image)
    }

    suspend fun hours(schedule: ShopSchedule?): String = when {
        schedule == null || (!schedule.isClosed && schedule.intervals.isEmpty()) -> text(Res.string.shop_schedule_unknown)
        schedule.isClosed -> text(Res.string.shop_schedule_closed)
        else -> schedule.intervals.joinToString("\n") {
            "${formatShopScheduleTime(it.openTime)} – ${formatShopScheduleTime(it.closeTime)}"
        }
    }

    suspend fun status(status: ShopScheduleStatus): String = when (status) {
        is ShopScheduleStatus.Open -> status.closingTime?.let { text(Res.string.shop_schedule_open_until, it) }
            ?: text(Res.string.shop_overview_open)
        is ShopScheduleStatus.Opens -> when (status.dayOffset) {
            0 -> text(Res.string.shop_schedule_opens_today, status.time)
            1 -> text(Res.string.shop_schedule_opens_tomorrow, status.time)
            else -> text(Res.string.shop_schedule_opens_day, text(shortDayName(status.dayOfWeek)), status.time)
        }
        ShopScheduleStatus.TodayClosed -> text(Res.string.shop_schedule_today_closed)
        ShopScheduleStatus.Closed -> text(Res.string.shop_overview_closed)
    }
}
