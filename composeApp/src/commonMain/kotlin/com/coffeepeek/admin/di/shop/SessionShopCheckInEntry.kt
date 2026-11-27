package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.createShopCheckInCreateEntry
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

/** Auth/session ownership remains in application composition, not feature API/domain. */
internal class SessionShopCheckInEntry(
    private val sessions: SessionReader,
    private val repository: ShopCheckInRepository,
    private val history: CheckInHistoryRepository,
    private val drafts: AppShopCheckInDraftStore,
    private val dates: ShopCheckInDateFormatter,
    private val photos: ShopCheckInPhotoPicker,
    private val onCheckInCreated: () -> Unit,
) : ShopCheckInCreateEntry {
    internal fun nativeForm(context: com.coffeepeek.feature.shop.api.ShopDetailContext, callbacks: ShopCheckInNativeCallbacks,
        shopSelectionIncluded: Boolean,
    ): NativeShopCheckInForm {
        val lease = ShopCheckInSession(sessions, repository, onCheckInCreated)
        val historyLease = CheckInHistorySession(sessions)
        val draft = lease.userId?.takeIf { lease.isCurrent() }?.let { drafts.forShop(context.shopId, context.shopSlug, it) }
        val form = draft?.let { ShopCheckInNativeForm(context.shopId, context.shopSlug, context.shopName, context.shopAddress,
            lease, it, dates, photos, HistoryShopCheckInPublicationReader(historyLease.bind(history)), lease::isCurrent,
            callbacks, shopSelectionIncluded) }
        return NativeShopCheckInForm(form, lease, historyLease)
    }
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
        shopSelectionIncluded: Boolean,
    ) {
        val lease = remember(shopId, shopSlug) { ShopCheckInSession(sessions, repository, onCheckInCreated) }
        val historyLease = remember(lease) { CheckInHistorySession(sessions) }
        val boundDrafts = remember(lease) {
            if (lease.isCurrent()) drafts.forShop(shopId, shopSlug, checkNotNull(lease.userId)) else null
        }
        DisposableEffect(lease, historyLease) { onDispose { lease.revoke(); historyLease.revoke() } }
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
                onDismiss()
            }
            return
        }
        val form = ShopCheckInNativeForm(shopId, shopSlug, shopName, shopAddress, lease, boundDrafts, dates, photos,
            HistoryShopCheckInPublicationReader(historyLease.bind(history)), lease::isCurrent,
            ShopCheckInNativeCallbacks(onDismiss, onChangeShop, { if (lease.isCurrent()) onSubmitted() },
                { if (lease.isCurrent()) onGoToFeed() }, { if (lease.isCurrent()) onOpenHistory() }), shopSelectionIncluded)
        SessionShopCheckInContent(form)
    }
}

internal class NativeShopCheckInForm(val form: ShopCheckInNativeForm?, private val lease: ShopCheckInSession,
    private val history: CheckInHistorySession,
) {
    fun close() {
        lease.revoke()
        history.revoke()
    }
}

@Composable
private fun SessionShopCheckInContent(form: ShopCheckInNativeForm) {
    AnalyticsOverlay {
        if (!PlatformShopCheckInForm(form)) {
            val entry = remember(form.repository) {
                createShopCheckInCreateEntry(form.repository, { _, _ -> form.drafts }, form.dates, form.photos,
                    analytics = { AppShopCheckInAnalytics(form.isCurrent) }, publication = form.publication)
            }
            entry.Content(form.shopId, form.shopSlug, form.shopName, form.shopAddress,
                form.callbacks.dismiss, form.callbacks.changeShop, form.callbacks.submitted,
                form.callbacks.goToFeed, form.callbacks.openHistory, form.shopSelectionIncluded)
        }
    }
}
