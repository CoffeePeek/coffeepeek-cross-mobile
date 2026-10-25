package com.coffeepeek.admin.ui.screen.review

import androidx.compose.runtime.Composable

interface ContentReportScreenRenderer {
    @Composable fun Content(id: String, isCheckIn: Boolean, isPreview: Boolean)
}

internal object LegacyContentReportScreenRenderer : ContentReportScreenRenderer {
    @Composable override fun Content(id: String, isCheckIn: Boolean, isPreview: Boolean) {
        ReviewReportScreen(reviewId = id, isPreview = isPreview, isCheckIn = isCheckIn)
    }
}
