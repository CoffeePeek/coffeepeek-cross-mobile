@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.di.shop.AppShopCheckInDraftStore
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

internal fun observeCheckInResumeDestination(sessions: SessionReader, drafts: AppShopCheckInDraftStore): Flow<Pair<String, String>?> =
    sessions.observeSession().map { it?.takeIf(sessions::isActiveSession)?.userId }.distinctUntilChanged().flatMapLatest { owner ->
        if (owner == null) flowOf(null) else drafts.observeLatestShop(owner).map { slug -> slug?.let { owner to it } }.catch {
            if (it is CancellationException) throw it
            emit(null)
        }
    }

internal fun activeShopResumeDestination(destination: Pair<String, String>?, sessions: SessionReader): Pair<String, String>? {
    val current = sessions.peekSession()
    return destination?.takeIf { current?.userId == it.first && sessions.isActiveSession(current) }
}

internal fun resumeShopCheckIn(destination: Pair<String, String>?, sessions: SessionReader) {
    activeShopResumeDestination(destination, sessions)?.let { Navigator.navigate(Navigator.Screen.ShopCheckInEditor(it.second)) }
}
