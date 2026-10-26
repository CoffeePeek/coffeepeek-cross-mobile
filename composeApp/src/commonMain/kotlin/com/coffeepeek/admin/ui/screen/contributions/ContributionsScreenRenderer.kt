package com.coffeepeek.admin.ui.screen.contributions

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.screen.addshop.AddShopScreen
import com.coffeepeek.admin.ui.screen.roaster.AddRoasterScreen
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeEditorScreen
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeRequestDetailScreen
import com.coffeepeek.admin.ui.screen.shopchange.SuggestShopChangeScreen

/** Platform selection only. iOS and shared code keep the legacy screens; Android composes the migrated entries. */
interface ContributionsScreenRenderer {
    /** [kind] is a [ContributionKind] name. */
    @Composable
    fun List(kind: ContributionKind)

    @Composable
    fun AddShop()

    @Composable
    fun AddRoaster()

    @Composable
    fun Suggest(shopId: String)

    /** [section] is a legacy section name; a blank [requestId] is a new request. */
    @Composable
    fun Editor(shopId: String, section: String, requestId: String)

    @Composable
    fun RequestDetail(requestId: String)
}

internal object LegacyContributionsScreenRenderer : ContributionsScreenRenderer {
    @Composable override fun List(kind: ContributionKind) = MyContributionsScreen(kind)
    @Composable override fun AddShop() = AddShopScreen()
    @Composable override fun AddRoaster() = AddRoasterScreen()
    @Composable override fun Suggest(shopId: String) = SuggestShopChangeScreen(shopId)
    @Composable override fun Editor(shopId: String, section: String, requestId: String) = ShopChangeEditorScreen(shopId, section, requestId)
    @Composable override fun RequestDetail(requestId: String) = ShopChangeRequestDetailScreen(requestId)
}
