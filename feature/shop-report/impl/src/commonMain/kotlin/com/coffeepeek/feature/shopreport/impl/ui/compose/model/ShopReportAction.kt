package com.coffeepeek.feature.shopreport.impl.ui.compose.model

import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory

internal sealed interface ShopReportAction {
    data class SelectCategory(val category: ShopIssueCategory) : ShopReportAction
    data class ChangeDescription(val description: String) : ShopReportAction
    data object Submit : ShopReportAction
    data object Back : ShopReportAction
}
