package com.coffeepeek.feature.shop.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.shop.impl.ui.compose.component.LocalShopAuthorImage
import com.coffeepeek.feature.shop.impl.ui.compose.component.LocalShopCheckInImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shop.api.ShopCheckInListEntry
import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_author_unknown
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInListViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInListScreen
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEvent

/** Repositories must be session-aware; app composition supplies auth and its monotonic generation. */
fun createShopCheckInListEntry(
    detailsRepository: ShopDetailsRepository,
    voteRepository: ShopCheckInVoteRepository,
    currentViewer: suspend () -> Result<ShopViewer>,
    authorImage: (@Composable (String, Modifier) -> Unit)? = null,
    checkInImage: (@Composable (String, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit)? = null,
): ShopCheckInListEntry = DefaultShopCheckInListEntry(detailsRepository, voteRepository, currentViewer, authorImage, checkInImage)

private class DefaultShopCheckInListEntry(
    private val detailsRepository: ShopDetailsRepository,
    private val voteRepository: ShopCheckInVoteRepository,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    private val authorImage: (@Composable (String, Modifier) -> Unit)?,
    private val checkInImage: (@Composable (String, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit)?,
) : ShopCheckInListEntry {
    @Composable
    override fun Content(
        shopId: String,
        sessionGeneration: Int,
        formatCheckInDate: (String) -> String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onRegister: () -> Unit,
        onReportCheckIn: (String) -> Unit,
        onOpenPhoto: (List<String>, List<String>, Int) -> Unit,
        onOpenAuthor: (String, String, String?) -> Unit,
        onBlockAuthor: (ShopCheckInAuthor) -> Unit,
        refreshGeneration: Long,
    ) {
        val unknownAuthor = stringResource(Res.string.shop_checkins_author_unknown)
        val viewModel = viewModel(key = "shop-check-in-list:$shopId") {
            ShopCheckInListViewModel(shopId, detailsRepository, voteRepository, currentViewer,
                sessionGeneration, refreshGeneration)
        }
        CompositionLocalProvider(LocalShopAuthorImage provides authorImage, LocalShopCheckInImage provides checkInImage) {
            ShopCheckInListScreen(viewModel, sessionGeneration, formatCheckInDate, refreshGeneration) { event ->
                when (event) {
                    ShopCheckInListEvent.Back -> onBack()
                    ShopCheckInListEvent.SignIn -> onSignIn()
                    ShopCheckInListEvent.Register -> onRegister()
                    is ShopCheckInListEvent.Report -> onReportCheckIn(event.checkInId)
                    is ShopCheckInListEvent.OpenPhoto -> onOpenPhoto(event.urls, event.originals, event.index)
                    is ShopCheckInListEvent.OpenAuthor -> onOpenAuthor(event.slug, event.nickname, event.avatarUrl)
                    is ShopCheckInListEvent.BlockAuthor -> onBlockAuthor(event.author.copy(
                        nickname = event.author.nickname.ifBlank { unknownAuthor }))
                }
            }
        }
    }
}
