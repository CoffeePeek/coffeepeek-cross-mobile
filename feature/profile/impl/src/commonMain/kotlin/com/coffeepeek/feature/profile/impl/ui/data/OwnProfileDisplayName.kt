package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.api.OwnProfile

/** Public names describe the person; the canonical URL slug is only a last fallback. */
internal fun ownProfileDisplayName(profile: OwnProfile?, publicNickname: String?): String? {
    profile ?: return null
    return profile.userName.takeIf(String::isNotBlank)
        ?: publicNickname?.takeIf(String::isNotBlank)
        ?: profile.address?.slug?.takeIf(String::isNotBlank).orEmpty()
}
