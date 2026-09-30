package com.coffeepeek.feature.shopreport.domain.repository

import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory

interface ShopIssueReportRepository {
    suspend fun submitReport(
        shopId: String,
        category: ShopIssueCategory,
        description: String?,
    ): Result<Unit>
}
