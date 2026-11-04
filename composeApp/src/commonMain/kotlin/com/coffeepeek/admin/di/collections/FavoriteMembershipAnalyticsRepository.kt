package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.AnalyticsScope
import com.coffeepeek.admin.analytics.AnalyticsService
import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.safeAnalyticsError
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
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
    private val favorites: AccountFavorites,
    private val sessions: SessionReader,
) : SavedCollectionsRepository by delegate {
    override suspend fun changeMemberships(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>): Result<List<SavedCollectionSummary>> {
        val attempt = try { prepare(target, added, removed) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        val result = delegate.changeMemberships(target, added, removed)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        currentCoroutineContext().ensureActive()
        if (attempt != null && attempt.lease == accountLease()) try { report(attempt, target, result) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Analytics cannot turn a confirmed write into a UI failure. */ }
        return result
    }

    private suspend fun prepare(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>): FavoriteAttempt? {
        val service = Analytics.service ?: return null
        val lease = accountLease() ?: return null
        val before = verifiedFavorite(target) ?: return null
        val lists = delegate.cachedMine(target) ?: delegate.mine(target).also { result ->
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        }.getOrNull() ?: return null
        if (lease != accountLease()) return null
        val desired = selectedAfterMembershipChange(lists, added, removed)
        if (before == desired) return null
        service.syncIdentityNow(lease.substringAfter(':'))
        val scope = service.operationScope()
        val entity = target.kind.entity()
        service.track(AnalyticsEvent.FAVORITE_ATTEMPTED, mapOf("entity_type" to entity.wire, "is_favorite" to desired), scope)
        return FavoriteAttempt(lease, service, scope, entity, before, TimeSource.Monotonic.markNow())
    }

    private suspend fun report(attempt: FavoriteAttempt, target: SavedCollectionTarget,
        result: Result<List<SavedCollectionSummary>>) {
        val duration = attempt.started.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
        val base = mapOf("entity_type" to attempt.entity.wire, "duration_ms" to duration, "favorite_storage" to "server")
        val error = result.exceptionOrNull()
        if (error != null) {
            val safe = safeAnalyticsError(error)
            if (safe.category != "cancelled") attempt.service.track(AnalyticsEvent.FAVORITE_FAILED, base + safe.properties(), attempt.scope)
            return
        }
        val after = verifiedFavorite(target) ?: return
        if (attempt.lease != accountLease() || after == attempt.before) return
        attempt.service.track(AnalyticsEvent.FAVORITE_CHANGED,
            base + mapOf("is_favorite" to after, "resumed_after_auth" to false), attempt.scope)
    }

    private suspend fun verifiedFavorite(target: SavedCollectionTarget): Boolean? {
        val result = favorites.getFavorites(refresh = true)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result.getOrNull()?.let { rows -> rows.any { it.matches(target) } }
    }

    private fun accountLease(): String? = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
        ?.takeIf(String::isNotBlank)?.let { "${sessions.accountGeneration()}:$it" }
}

private data class FavoriteAttempt(val lease: String, val service: AnalyticsService, val scope: AnalyticsScope,
    val entity: Entity, val before: Boolean, val started: TimeMark)

internal fun selectedAfterMembershipChange(lists: List<SavedCollectionSummary>, added: Set<String>, removed: Set<String>): Boolean {
    val current = lists.filter { it.containsItem }.map { it.id }.toSet()
    return ((current + added) - removed).isNotEmpty()
}

internal fun FavoriteEntry.matches(target: SavedCollectionTarget): Boolean =
    address?.slug == target.slug && kind == when (target.kind) {
        CollectionPlaceKind.CoffeeShop -> FavoriteKind.CoffeeShop
        CollectionPlaceKind.Roaster -> FavoriteKind.Roaster
    }

private fun CollectionPlaceKind.entity(): Entity = when (this) {
    CollectionPlaceKind.CoffeeShop -> Entity.Shop
    CollectionPlaceKind.Roaster -> Entity.Roaster
}
