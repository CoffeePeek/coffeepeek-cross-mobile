package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.notifications.api.NotificationDestination
import com.coffeepeek.feature.notifications.api.NotificationFeature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Application-owned adapters between independent feature contracts and root navigation. */
internal class NotificationNavigation(
    private val notifications: NotificationFeature,
    private val contributions: ContributionsRepository,
    private val currentUserId: () -> String?,
    private val navigate: (Navigator.Screen) -> Unit,
    private val onError: suspend () -> Unit,
) {
    private val opening = Mutex()

    suspend fun open(id: String, accountId: String, generation: Long): Boolean = opening.withLock {
        if (!current(accountId, generation)) return@withLock false
        try {
            val destination = screen(notifications.destination(id)) { current(accountId, generation) }
            currentCoroutineContext().ensureActive()
            if (!current(accountId, generation)) return@withLock false
            notifications.markRead(id)
            navigate(destination)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (current(accountId, generation)) onError()
            false
        }
    }

    private fun current(accountId: String, generation: Long): Boolean =
        currentUserId() == accountId && notifications.state.value.let {
            it.accountId == accountId && it.generation == generation
        }

    private suspend fun screen(destination: NotificationDestination?, canAccess: () -> Boolean): Navigator.Screen = when (destination) {
        is NotificationDestination.Shop -> shop(destination.slug)
        is NotificationDestination.ShopChange -> changedShop(destination.requestId, canAccess)
        is NotificationDestination.ShopSubmission -> publishedShop(destination.submissionId, canAccess)?.let(::shop)
            ?: Navigator.Screen.MyContributions(ContributionKind.Shops.name)
        NotificationDestination.RoasterSubmissions -> Navigator.Screen.MyContributions(ContributionKind.Roasters.name)
        NotificationDestination.CheckIns -> Navigator.Screen.VisitedPlaces
        null -> Navigator.Screen.Main
    }

    private suspend fun changedShop(id: String, canAccess: () -> Boolean): Navigator.Screen {
        check(canAccess())
        val change = contributions.change(id).getOrThrow()
        check(canAccess())
        return shop(change.shopSlug)
    }

    private fun shop(slug: String): Navigator.Screen.ShopDetail {
        require(slug.length in 1..100 && slug.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*")))
        return Navigator.Screen.ShopDetail(slug)
    }

    private suspend fun publishedShop(id: String, canAccess: () -> Boolean): String? {
        for (status in listOf(ModerationStatus.Approved, ModerationStatus.Pending, ModerationStatus.Rejected)) {
            var page = 1
            do {
                check(canAccess())
                val result = contributions.shops(status, page, 100).getOrThrow()
                check(canAccess())
                require(result.currentPage == page && result.totalPages <= 10_000)
                result.items.firstOrNull { it.id == id }?.let { return it.publishedShopSlug }
                page++
            } while (result.hasMore)
        }
        return null
    }
}
