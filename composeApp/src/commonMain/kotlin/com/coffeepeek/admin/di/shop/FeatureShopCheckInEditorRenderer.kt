package com.coffeepeek.admin.di.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_shop_unavailable
import coffeepeek.composeapp.generated.resources.shop_session_retry
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInEditorRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInFormRenderer
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.CancellationException

internal class FeatureShopCheckInEditorRenderer(
    private val sessions: SessionReader,
    private val details: ShopDetailsRepository,
    private val form: ShopCheckInFormRenderer,
) : ShopCheckInEditorRenderer {
    @Composable
    override fun Content(shopId: String) {
        ShopBrowsingHost("shop-check-in-editor:$shopId", sessions) { session ->
            var retry by remember(session, shopId) { mutableIntStateOf(0) }
            val detailsResult by produceState<Result<ShopDetails>?>(null, session, shopId, retry) {
                value = session.lease.details(details).getDetails(shopId).also {
                    it.exceptionOrNull()?.let { error -> if (error is CancellationException) throw error }
                }
            }
            val overview = detailsResult?.getOrNull()?.overview
            when {
                overview != null -> form.Content(shopId, overview.id, overview.title, overview.address,
                    onDismiss = Navigator::popBack,
                    onChangeShop = { Navigator.popThenNavigate(Navigator.Screen.CreateCheckIn) },
                    onSubmitted = {},
                    onGoToFeed = { Navigator.popThenSelectTab(Navigator.Screen.CommunityTab) },
                    onOpenHistory = { Navigator.popThenNavigate(Navigator.Screen.VisitedPlaces) })
                detailsResult == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader()
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(Modifier.padding(CoffeePeekDimens.spacing6),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                        Text(stringResource(Res.string.check_in_shop_unavailable), color = MaterialTheme.colorScheme.error)
                        CoffeePeekButton(stringResource(Res.string.shop_session_retry), { retry++ })
                    }
                }
            }
        }
    }
}
