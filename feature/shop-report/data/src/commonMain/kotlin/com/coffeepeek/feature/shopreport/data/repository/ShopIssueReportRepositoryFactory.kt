package com.coffeepeek.feature.shopreport.data.repository

import com.coffeepeek.feature.shopreport.data.backend.CreateShopIssueReportRequest
import com.coffeepeek.feature.shopreport.data.backend.ShopIssueReportBackend
import com.coffeepeek.feature.shopreport.data.mapper.toBackend
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import io.ktor.client.HttpClient

/** Application composition supplies the configured, authenticated client. */
fun createShopIssueReportRepository(client: HttpClient): ShopIssueReportRepository =
    DefaultShopIssueReportRepository(ShopIssueReportBackend(client))

private class DefaultShopIssueReportRepository(
    private val backend: ShopIssueReportBackend,
) : ShopIssueReportRepository {
    override suspend fun submitReport(
        shopId: String,
        category: ShopIssueCategory,
        description: String?,
    ): Result<Unit> = backend.submit(CreateShopIssueReportRequest(shopId, category.toBackend(), description))
}
