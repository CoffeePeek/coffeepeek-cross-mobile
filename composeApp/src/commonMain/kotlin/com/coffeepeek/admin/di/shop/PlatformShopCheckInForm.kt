package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPublicationReader
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker

/** Composition-owned dependencies are already bound to the opening account and shop. */
internal class ShopCheckInNativeForm(
    val shopId: String, val shopSlug: String, val shopName: String, val shopAddress: String?,
    val repository: ShopCheckInRepository, val drafts: ShopCheckInDraftStore,
    val dates: ShopCheckInDateFormatter, val photos: ShopCheckInPhotoPicker,
    val publication: ShopCheckInPublicationReader, val isCurrent: () -> Boolean,
    val callbacks: ShopCheckInNativeCallbacks,
)

internal class ShopCheckInNativeCallbacks(
    val dismiss: () -> Unit, val changeShop: () -> Unit, val submitted: () -> Unit,
    val goToFeed: () -> Unit, val openHistory: () -> Unit,
)

@Composable
internal expect fun PlatformShopCheckInForm(form: ShopCheckInNativeForm): Boolean
