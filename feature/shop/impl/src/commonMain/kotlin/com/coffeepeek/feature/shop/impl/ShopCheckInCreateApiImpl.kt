package com.coffeepeek.feature.shop.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shop.api.ShopCheckInAnalytics
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPublicationReader
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInCreateViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInCreateScreen
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker

fun createShopCheckInCreateEntry(
    repository: ShopCheckInRepository,
    draftsForShop: (shopId: String, shopSlug: String) -> ShopCheckInDraftStore,
    dates: ShopCheckInDateFormatter,
    photos: ShopCheckInPhotoPicker,
    analytics: () -> ShopCheckInAnalytics = { ShopCheckInAnalytics.None },
    publication: ShopCheckInPublicationReader = ShopCheckInPublicationReader.None,
): ShopCheckInCreateEntry {
    return DefaultShopCheckInCreateEntry(repository, draftsForShop, dates, photos, analytics, publication)
}

private class DefaultShopCheckInCreateEntry(
    private val repository: ShopCheckInRepository,
    private val draftsForShop: (String, String) -> ShopCheckInDraftStore,
    private val dates: ShopCheckInDateFormatter,
    private val photos: ShopCheckInPhotoPicker,
    private val analytics: () -> ShopCheckInAnalytics,
    private val publication: ShopCheckInPublicationReader,
) : ShopCheckInCreateEntry {
    @Composable
    override fun Content(
        shopId: String,
        shopSlug: String,
        shopName: String,
        shopAddress: String?,
        onDismiss: () -> Unit,
        onChangeShop: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
    ) {
        val owner = rememberCheckInOwner(shopId, shopSlug)
        val model = checkInViewModel(owner, shopId, shopSlug)
        ShopCheckInCreateScreen(model, shopName, shopAddress, dates, photos, onChangeShop) { event ->
            when (event) {
                ShopCheckInFormEvent.Dismiss -> onDismiss()
                is ShopCheckInFormEvent.Submitted -> onSubmitted()
                ShopCheckInFormEvent.GoToFeed -> onGoToFeed()
                ShopCheckInFormEvent.OpenHistory -> onOpenHistory()
            }
        }
    }

    @Composable
    private fun checkInViewModel(owner: ViewModelStoreOwner, shopId: String, shopSlug: String): ShopCheckInCreateViewModel =
        viewModel(viewModelStoreOwner = owner) {
            ShopCheckInCreateViewModel(
                initial = ShopCheckInCreateInput(shopSlug, "", ShopRating(4, 4, 4), dates.visitInstant(dates.nowMillis())),
                repository = repository,
                drafts = draftsForShop(shopId, shopSlug),
                analytics = analytics(),
                publication = publication,
            )
        }
}

/** The saved draft outlives this per-opening ViewModel. */
@Composable
private fun rememberCheckInOwner(shopId: String, shopSlug: String): ViewModelStoreOwner {
    val owner = remember(shopId, shopSlug) {
        object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    return owner
}
