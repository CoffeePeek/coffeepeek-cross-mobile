package com.coffeepeek.admin.di.discovery

import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.impl.createDiscoveryTabEntry
import com.coffeepeek.feature.discovery.impl.createCheckInShopPickerEntry
import com.coffeepeek.feature.discovery.impl.ui.NativeDiscoverySession
import com.coffeepeek.feature.discovery.impl.ui.createNativeDiscoverySession
import com.coffeepeek.admin.di.shop.ShopBrowsingHostSession
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession

internal fun interface NativeDiscoverySessionFactory {
    fun createNativeSession(): NativeDiscoverySession
}

internal fun interface NativeCheckInShopPickerSessionFactory {
    fun createNativeSession(session: ShopBrowsingHostSession, history: CheckInHistorySession): NativeDiscoverySession
}

/** One application assembly of the existing ports for either platform entry. */
internal class AppDiscoverySessions(private val repository: DiscoveryRepository, private val favorites: DiscoveryFavorites,
    private val city: DiscoveryCity, private val location: DiscoveryLocation, private val account: DiscoveryAccount,
    private val details: DiscoveryShopDetails) {
    fun nativeSession(picker: Boolean = false, recentShops: DiscoveryRecentShops? = null) =
        createNativeDiscoverySession(repository, favorites, city, location, account, details, AppDiscoveryAnalytics(), picker, recentShops)
    fun entry(host: DiscoveryHost, roasters: DiscoveryRoasterSlots): DiscoveryTabEntry = createDiscoveryTabEntry(
        repository, favorites, city, location, account, details, AppDiscoveryAnalytics(), host, roasters)
    fun pickerEntry(host: DiscoveryHost, recentShops: DiscoveryRecentShops) = createCheckInShopPickerEntry(
        repository, favorites, city, location, account, details, AppDiscoveryAnalytics(), host, recentShops)
}
