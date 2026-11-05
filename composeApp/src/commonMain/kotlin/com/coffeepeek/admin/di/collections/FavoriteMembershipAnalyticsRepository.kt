package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.AnalyticsScope
import com.coffeepeek.admin.analytics.AnalyticsService
import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.safeAnalyticsError
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoriteTarget
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SavedCollectionMembershipResult
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionTarget
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Only a confirmed picker write can report a favorite mutation; list metadata edits are excluded. */
internal class FavoriteMembershipAnalyticsRepository(
    private val delegate: SavedCollectionsRepository,
    private val favorites: FavoritesRepository,
    private val sessions: SessionReader,
) : SavedCollectionsRepository by delegate {
    override suspend fun changeMemberships(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>): Result<SavedCollectionMembershipResult> {
        if (added.isEmpty() && removed.isEmpty()) return Result.success(SavedCollectionMembershipResult(delegate.cachedMine(target).orEmpty(), membershipChanged = false))
        val lease = accountLease() ?: throw CancellationException("Collections account changed")
        val attempt = prepareSafely(target, added, removed, lease)
        if (lease != accountLease()) throw CancellationException("Collections account changed")
        val result = delegate.changeMemberships(target, added, removed)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        currentCoroutineContext().ensureActive()
        if (lease != accountLease()) throw CancellationException("Collections account changed")
        result.onSuccess { receipt ->
            receipt.confirmedFavorite()?.let { favorite ->
                favorites.applyConfirmedMembership(lease.account, lease.generation, target.favoriteTarget(), favorite)
            }
        }
        if (attempt != null) reportSafely(attempt, result)
        return result
    }

    private fun prepareSafely(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>, lease: FavoriteLease): FavoriteAttempt? =
        try { prepare(target, added, removed, lease) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }

    private fun reportSafely(attempt: FavoriteAttempt, result: Result<SavedCollectionMembershipResult>) {
        try { report(attempt, result) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Confirmed membership must not fail because analytics delivery failed. */ }
    }

    private fun prepare(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>, lease: FavoriteLease): FavoriteAttempt? {
        val service = Analytics.service ?: return null
        val lists = delegate.cachedMine(target) ?: return null
        val before = favorites.cachedMembership(target.favoriteTarget()) ?: return null
        if (lease != accountLease()) return null
        val desired = selectedAfterMembershipChange(lists, added, removed)
        if (before == desired) return null
        service.syncIdentityNow(lease.account)
        val scope = service.operationScope()
        val entity = target.kind.entity()
        service.track(AnalyticsEvent.FAVORITE_ATTEMPTED, mapOf("entity_type" to entity.wire, "is_favorite" to desired), scope)
        return FavoriteAttempt(lease, service, scope, entity, before, TimeSource.Monotonic.markNow())
    }

    private fun report(attempt: FavoriteAttempt, result: Result<SavedCollectionMembershipResult>) {
        val duration = attempt.started.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
        val base = mapOf("entity_type" to attempt.entity.wire, "duration_ms" to duration, "favorite_storage" to "server")
        val error = result.exceptionOrNull()
        if (error != null) {
            val safe = safeAnalyticsError(error)
            if (safe.category != "cancelled") attempt.service.track(AnalyticsEvent.FAVORITE_FAILED, base + safe.properties(), attempt.scope)
            return
        }
        val receipt = result.getOrNull()?.takeIf { it.membershipChanged == true } ?: return
        val after = receipt.confirmedFavorite() ?: return
        if (attempt.lease != accountLease() || after == attempt.before) return
        attempt.service.track(AnalyticsEvent.FAVORITE_CHANGED,
            base + mapOf("is_favorite" to after, "resumed_after_auth" to false), attempt.scope)
    }

    private fun accountLease(): FavoriteLease? = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
        ?.takeIf(String::isNotBlank)?.let { FavoriteLease(it, sessions.accountGeneration()) }
}

/** An existing membership proves a heart; an empty union cannot rule out legacy private favorites. */
private fun SavedCollectionMembershipResult.confirmedFavorite(): Boolean? =
    isFavorite ?: true.takeIf { lists.any { it.containsItem } }

private data class FavoriteLease(val account: String, val generation: Long)
private data class FavoriteAttempt(val lease: FavoriteLease, val service: AnalyticsService, val scope: AnalyticsScope,
    val entity: Entity, val before: Boolean, val started: TimeMark)

internal fun selectedAfterMembershipChange(lists: List<SavedCollectionSummary>, added: Set<String>, removed: Set<String>): Boolean {
    val current = lists.filter { it.containsItem }.map { it.id }.toSet()
    return ((current + added) - removed).isNotEmpty()
}

internal fun FavoriteEntry.matches(target: SavedCollectionTarget): Boolean =
    slug == target.slug && kind == when (target.kind) {
        CollectionPlaceKind.CoffeeShop -> FavoriteKind.CoffeeShop
        CollectionPlaceKind.Roaster -> FavoriteKind.Roaster
    }

private fun CollectionPlaceKind.entity(): Entity = when (this) {
    CollectionPlaceKind.CoffeeShop -> Entity.Shop
    CollectionPlaceKind.Roaster -> Entity.Roaster
}

private fun SavedCollectionTarget.favoriteTarget() = FavoriteTarget(
    if (kind == CollectionPlaceKind.CoffeeShop) FavoriteKind.CoffeeShop else FavoriteKind.Roaster, slug,
)
