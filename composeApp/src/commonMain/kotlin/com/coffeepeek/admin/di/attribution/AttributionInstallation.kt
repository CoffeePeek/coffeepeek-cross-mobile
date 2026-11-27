package com.coffeepeek.admin.di.attribution

import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.AnalyticsService
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.attribution.data.createAttributionFeature
import com.coffeepeek.feature.attribution.domain.AttributionIdentity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun installAttribution(service: AnalyticsService, gateway: ApiClients, sessions: SessionReader, scope: CoroutineScope) {
    val feature = createAttributionFeature(gateway, sessions, "${Locator.appFilesRootPath}/acquisition", Locator.platformContext, service::invalidatePermission)
    AppAttribution.feature = feature
    AppAttribution.actionScope = scope
    AppAttribution.sessions = sessions
    scope.launch { observeAttribution(service, sessions) }
}

private suspend fun observeAttribution(service: AnalyticsService, sessions: SessionReader) {
    var account: String? = null
    service.state.map { listOf(it.userId, it.generation, it.ready, it.foreground, it.consent, it.restored) }
        .distinctUntilChanged().collectLatest {
            val next = service.state.value.userId
            val changed = account != null && account != next
            account = next
            reconcileAttribution(service, sessions, changed)
        }
}

private suspend fun reconcileAttribution(service: AnalyticsService, sessions: SessionReader, changed: Boolean) {
    val feature = AppAttribution.feature ?: return
    val state = service.state.value
    try {
        val revoked = state.consent == AnalyticsConsent.Declined && state.restored
        if (changed || revoked) feature.clearAccount(revoked)
        val permit = permittedIdentity(service, sessions)
        if (permit == null) feature.pause() else maintainAttribution(service, sessions)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        feature.pause()
    }
}

private suspend fun maintainAttribution(service: AnalyticsService, sessions: SessionReader) {
    while (true) {
        val identity = permittedIdentity(service, sessions) ?: return
        try { AppAttribution.feature?.sync(identity) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Retry permitted work on the next foreground interval. */ }
        delay(60_000)
    }
}

internal fun permittedIdentity(service: AnalyticsService, sessions: SessionReader): AttributionIdentity? {
    val state = service.state.value
    val sdk = service.attributionIdentity() ?: return null
    val session = sessions.peekSession()?.takeIf { it.userId == state.userId } ?: return null
    val generation = sessions.accountGeneration()
    return AttributionIdentity(requireNotNull(session.userId), sdk.distinctId, sdk.sessionId, AppConfig.updatePlatform,
        sdk.appVersion, sdk.environment) {
        val current = service.state.value
        service.isAttributionLeaseCurrent(sdk.revision) && current.consentRevision == state.consentRevision &&
            current.generation == state.generation && current.ready && current.foreground &&
            current.consent == AnalyticsConsent.Accepted && current.consentPersisted &&
            sessions.accountGeneration() == generation && sessions.peekSession()?.userId == session.userId &&
            sessions.isActiveSession(sessions.peekSession()) && current.userId == session.userId
    }
}
