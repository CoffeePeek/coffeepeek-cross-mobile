package com.coffeepeek.feature.shop.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
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
): ShopCheckInCreateEntry = DefaultShopCheckInCreateEntry(repository, draftsForShop, dates, photos)

private class DefaultShopCheckInCreateEntry(
    private val repository: ShopCheckInRepository,
    private val draftsForShop: (String, String) -> ShopCheckInDraftStore,
    private val dates: ShopCheckInDateFormatter,
    private val photos: ShopCheckInPhotoPicker,
) : ShopCheckInCreateEntry {
    @Composable
    override fun Content(
        shopId: String,
        shopSlug: String,
        shopName: String,
        onDismiss: () -> Unit,
        onSubmitted: () -> Unit,
        onGoToFeed: () -> Unit,
        onOpenHistory: () -> Unit,
    ) {
        // Each modal opening gets a fresh lifecycle VM, not the completed VM of
        // the underlying nav entry. Disposal cancels work; the process draft survives.
        val owner = remember(shopId, shopSlug) {
            object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        }
        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
        val model = viewModel(viewModelStoreOwner = owner) {
            ShopCheckInCreateViewModel(
                initial = ShopCheckInCreateInput(shopSlug, "", ShopRating(4, 4, 4), dates.visitInstant(dates.nowMillis())),
                repository = repository,
                drafts = draftsForShop(shopId, shopSlug),
            )
        }
        ShopCheckInCreateScreen(model, shopName, dates, photos) { event ->
            when (event) {
                ShopCheckInFormEvent.Dismiss -> onDismiss()
                is ShopCheckInFormEvent.Submitted -> onSubmitted()
                ShopCheckInFormEvent.GoToFeed -> onGoToFeed()
                ShopCheckInFormEvent.OpenHistory -> onOpenHistory()
            }
        }
    }
}
