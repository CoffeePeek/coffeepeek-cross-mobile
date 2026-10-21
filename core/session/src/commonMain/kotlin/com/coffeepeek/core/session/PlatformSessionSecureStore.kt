package com.coffeepeek.core.session

/** Android: encrypted preferences (needs a Context). iOS: Keychain. */
expect fun createPlatformSessionSecureStore(
    platformContext: Any?,
    legacyRows: LegacyCredentialRows,
): SessionSecureStore
