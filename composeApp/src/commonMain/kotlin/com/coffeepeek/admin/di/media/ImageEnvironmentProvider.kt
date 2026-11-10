package com.coffeepeek.admin.di.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.coffeepeek.admin.analytics.AnalyticsOcclusion
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.component.platformReduceMotion
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerPlatform
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekPhotoViewerPlatform
import com.coffeepeek.core.media.ImageEnvironment
import com.coffeepeek.core.media.LocalImageEnvironment
import com.coffeepeek.core.session.SessionReader
import org.koin.compose.koinInject

/** Root assembly only: image services and the gallery's platform/analytics hooks. */
@Composable
internal fun ProvideImageEnvironment(content: @Composable () -> Unit) {
    val sessions = koinInject<SessionReader>()
    val baseUrl = Constants.BASE_URL
    val images = remember(sessions, baseUrl) { ImageEnvironment(baseUrl, sessions) }
    val viewer = remember { CoffeePeekPhotoViewerPlatform(overlay = { AnalyticsOcclusion() }, reduceMotion = { platformReduceMotion() }) }
    CompositionLocalProvider(LocalImageEnvironment provides images, LocalCoffeePeekPhotoViewerPlatform provides viewer, content = content)
}
