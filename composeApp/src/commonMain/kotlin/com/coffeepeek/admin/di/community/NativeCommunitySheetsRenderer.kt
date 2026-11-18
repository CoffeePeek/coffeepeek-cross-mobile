package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.community.api.*

internal typealias NativeCommunityEditorRenderer = @Composable (CommunityEditorPresentation, CommunityEditorActions) -> Unit
internal typealias NativeCommunityReportRenderer = @Composable (CommunityReportPresentation, CommunityReportActions) -> Unit

internal expect fun nativeCommunityEditorRenderer(): NativeCommunityEditorRenderer?
internal expect fun nativeCommunityReportRenderer(): NativeCommunityReportRenderer?
