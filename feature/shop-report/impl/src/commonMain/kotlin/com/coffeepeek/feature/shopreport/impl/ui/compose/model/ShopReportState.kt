package com.coffeepeek.feature.shopreport.impl.ui.compose.model

import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory

internal data class ShopReportState(
    val selectedCategory: ShopIssueCategory? = null,
    val description: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: ShopReportError? = null,
)

internal enum class ShopReportError { CategoryRequired, DescriptionRequired, SubmissionFailed }
