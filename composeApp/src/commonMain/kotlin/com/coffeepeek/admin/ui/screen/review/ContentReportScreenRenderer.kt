package com.coffeepeek.admin.ui.screen.review

import androidx.compose.runtime.Composable

interface ContentReportScreenRenderer {
    @Composable fun Content(id: String, isCheckIn: Boolean, isPreview: Boolean)
}
