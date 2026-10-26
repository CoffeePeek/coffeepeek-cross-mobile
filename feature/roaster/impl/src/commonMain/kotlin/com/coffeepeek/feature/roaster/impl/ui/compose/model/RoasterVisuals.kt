package com.coffeepeek.feature.roaster.impl.ui.compose.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit

/** Internal renderer seams for stateless content/previews; none are application entry contracts. */
internal typealias RoasterImage = @Composable (String, String, Modifier, ContentScale, TextUnit) -> Unit
internal typealias RoasterLoadError = @Composable (onRetry: () -> Unit) -> Unit
internal typealias RoasterPhotoViewer = @Composable (url: String, onDismiss: () -> Unit) -> Unit
