package com.coffeepeek.feature.attribution.api

import com.coffeepeek.feature.attribution.domain.AttributionIdentity

/** Feature UI can report an actual loaded destination without access to HTTP or SDKs. */
interface AttributionSignals {
    suspend fun visible(canonicalPath: String)
}

interface AttributionSharing {
    suspend fun share(canonicalPath: String, placement: String): Result<String>
}

/** Application composition owns permission and functional navigation. Native guests remain untracked. */
interface AttributionFeature : AttributionSignals, AttributionSharing {
    suspend fun open(code: String): Result<String>
    suspend fun sync(identity: AttributionIdentity)
    suspend fun pause()
    suspend fun clearAccount(resetInstallation: Boolean)
    suspend fun recoverHandoff(token: String, transport: String)
}
