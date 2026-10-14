package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.createShopCheckInCreateEntry
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

/** Auth/session ownership remains in Android composition, not feature API/domain. */
internal class AndroidSessionShopCheckInEntry(
    private val sessions: SessionRepository,
    private val repository: ShopCheckInRepository,
    private val drafts: AndroidShopCheckInDraftStore,
    private val dates: ShopCheckInDateFormatter,
    private val photos: ShopCheckInPhotoPicker,
    private val drinkPhotos: ShopDrinkPhotoProcessor? = null,
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
        val lease = remember(shopId, shopSlug) { AndroidShopCheckInSession(sessions, repository) }
        val boundDrafts = remember(lease) {
            if (lease.isCurrent()) drafts.forShop(shopId, shopSlug, lease.userId) else null
        }
        DisposableEffect(lease) { onDispose { lease.revoke() } }
        val session by remember(sessions) {
            sessions.observeSession().catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }
        }
            .collectAsState(initial = sessions.peekSession())
        if (boundDrafts == null || !lease.isCurrent(session) || !lease.isCurrent()) {
            LaunchedEffect(lease) {
                lease.revoke()
                // Only this binding's draft can be cleared; a replacement stays intact.
                boundDrafts?.clear(shopSlug)
                onDismiss()
            }
            return
        }
        val entry = remember(lease) {
            createShopCheckInCreateEntry(lease, { _, _ -> boundDrafts }, dates, photos, drinkPhotos)
        }
        entry.Content(
            shopId, shopSlug, shopName,
            onDismiss = onDismiss,
            onSubmitted = { if (lease.isCurrent()) onSubmitted() },
            onGoToFeed = { if (lease.isCurrent()) onGoToFeed() },
            onOpenHistory = { if (lease.isCurrent()) onOpenHistory() },
        )
    }
}
