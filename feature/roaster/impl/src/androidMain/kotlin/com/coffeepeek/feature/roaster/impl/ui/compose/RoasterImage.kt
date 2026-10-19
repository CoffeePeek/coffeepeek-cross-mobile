package com.coffeepeek.feature.roaster.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit

/** Application image port: owns loading, caching and the failure placeholder (sized by [labelSize]). */
internal typealias RoasterImage = @Composable (url: String, description: String, modifier: Modifier, scale: ContentScale, labelSize: TextUnit) -> Unit

/** Application error content (mascot + retry), so the feature does not duplicate app artwork. */
internal typealias RoasterLoadError = @Composable (onRetry: () -> Unit) -> Unit
