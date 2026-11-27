package com.coffeepeek.admin.di.discovery

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.*

class IosDiscoveryAccountTest {
    @Test fun tokenRefreshRetainsNativeAccountButLogoutAndReplacementRevokeIt() {
        val reader = DiscoveryAccounts()
        val original = reader.discoveryAccount()
        reader.session.value = reader.session.value?.copy(accessToken = "refreshed")
        assertEquals(original, reader.discoveryAccount())
        reader.session.value = Session("other-token", userId = "other")
        assertNotEquals(original, reader.discoveryAccount())
        reader.session.value = null
        assertNotEquals(original, reader.discoveryAccount())
    }

    @Test fun anotherLoginLifetimeRevokesSameUserAndAuthenticationChangesAreObserved() {
        val reader = DiscoveryAccounts()
        val original = reader.discoveryAccount()
        reader.generation++
        assertNotEquals(original, reader.discoveryAccount())
        reader.generation--
        reader.active = false
        assertNotEquals(original, reader.discoveryAccount())
    }
}

private class DiscoveryAccounts : SessionReader {
    val session = MutableStateFlow<Session?>(Session("fixture-token", userId = "owner"))
    var generation = 1L
    var active = true
    override fun accountGeneration() = generation
    override fun peekSession() = session.value
    override fun observeSession() = session
    override suspend fun getSession() = session.value
    override fun isActiveSession(session: Session?) = active && session != null
}
