package com.coffeepeek.admin.map

import androidx.compose.runtime.staticCompositionLocalOf

/** Optional inline/offline style supplied by application composition; live defaults are unchanged. */
internal val LocalMapStyleOverride = staticCompositionLocalOf<String?> { null }
