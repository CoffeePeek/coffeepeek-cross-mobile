package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.analytics.*
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

/** These migrated adapters bypass TrackedFavoriteInteractions; report their single actual write. */
internal suspend fun trackFavoriteMutation(
    entity: Entity,
    desired: Boolean,
    action: suspend () -> Result<Unit>,
): Result<Unit> {
    val service = Analytics.service
    val scope = service?.operationScope()
    val started = TimeSource.Monotonic.markNow()
    Analytics.track(AnalyticsEvent.FAVORITE_ATTEMPTED, "entity_type" to entity.wire, "is_favorite" to desired)
    val result = try { action() } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    if (service != null && scope != null) reportFavoriteResult(service, scope, entity, desired,
        started.elapsedNow().inWholeMilliseconds.coerceAtLeast(0), result)
    return result
}

private fun reportFavoriteResult(service: AnalyticsService, scope: AnalyticsScope, entity: Entity,
    desired: Boolean, duration: Long, result: Result<Unit>) {
    val base = mapOf("entity_type" to entity.wire, "duration_ms" to duration, "favorite_storage" to "server")
    result.onSuccess {
        service.track(AnalyticsEvent.FAVORITE_CHANGED,
            base + mapOf("is_favorite" to desired, "resumed_after_auth" to false), scope)
    }.onFailure {
        val error = safeAnalyticsError(it)
        if (error.category != "cancelled") service.track(AnalyticsEvent.FAVORITE_FAILED, base + error.properties(), scope)
    }
}
