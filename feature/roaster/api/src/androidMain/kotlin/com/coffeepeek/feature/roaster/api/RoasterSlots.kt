package com.coffeepeek.feature.roaster.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit

/** Application image port: owns loading, caching and the failure placeholder (sized by [labelSize]). */
typealias RoasterImage = @Composable (url: String, description: String, modifier: Modifier, scale: ContentScale, labelSize: TextUnit) -> Unit

/** Application error content (mascot + retry), so the feature does not duplicate app artwork. */
typealias RoasterLoadError = @Composable (onRetry: () -> Unit) -> Unit

/** Application viewer for a single photo URL (full-screen dialog). */
typealias RoasterPhotoViewer = @Composable (url: String, onDismiss: () -> Unit) -> Unit
