package com.coffeepeek.feature.profile.api

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Application image port: owns loading, caching and the failure state. */
typealias ProfileImage = @Composable (url: String, description: String?, modifier: Modifier) -> Unit

/** Application full-screen viewer. [originals] parallel [previews]; the viewer upgrades to an original when it differs from its preview. */
typealias ProfilePhotoViewer = @Composable (previews: List<String>, originals: List<String>, initialIndex: Int, onDismiss: () -> Unit) -> Unit

/** Warms image caches for the photos of the visible check-ins: pairs of (check-in id, preview urls). */
typealias ProfilePhotoPrefetch = @Composable (listState: LazyListState, items: List<Pair<String, List<String>>>) -> Unit

class ProfileSlots(
    val image: ProfileImage,
    val photoViewer: ProfilePhotoViewer,
    val photoPrefetch: ProfilePhotoPrefetch,
    /** Fit-scaled logo loading; the regular image port crops covers and avatars. */
    val logoImage: ProfileImage = image,
    val displayDate: @Composable (String) -> String = { it },
    val progressiveImage: @Composable (preview: String, detail: String?, modifier: Modifier) -> Unit = { preview, _, modifier -> image(preview, null, modifier) },
    val reportCheckIn: (id: String) -> Unit = {},
)
