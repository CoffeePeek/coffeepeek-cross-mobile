package com.coffeepeek.admin.di.discovery

import com.coffeepeek.core.session.SessionReader

/** Login lifetime, including readers that cannot provide a generation counter. Token refresh preserves this key. */
internal data class IosDiscoveryAccount(val generation: Long, val userId: String?, val signedIn: Boolean)

internal fun SessionReader.discoveryAccount(): IosDiscoveryAccount {
    val session = peekSession()
    return IosDiscoveryAccount(accountGeneration(), session?.userId, isActiveSession(session))
}
