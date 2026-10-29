package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.runtime.staticCompositionLocalOf
internal data class RoasterBusinessIdentity(val id: String, val slug: String, val canonicalPath: String)
internal val LocalRoasterBusinessIdentity = staticCompositionLocalOf<RoasterBusinessIdentity?> { null }
