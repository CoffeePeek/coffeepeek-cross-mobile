package com.coffeepeek.feature.shopreport.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shopreport.api.ShopReportEntry
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.ui.ShopReportViewModel
import com.coffeepeek.feature.shopreport.impl.ui.compose.ShopReportScreen

fun createShopReportEntry(repository: ShopIssueReportRepository): ShopReportEntry =
    ShopReportApiImpl(repository)

private class ShopReportApiImpl(
    private val repository: ShopIssueReportRepository,
) : ShopReportEntry {
    @Composable
    override fun Content(shopId: String, shopTitle: String, onBack: () -> Unit) {
        val viewModel = viewModel { ShopReportViewModel(shopId, repository) }
        ShopReportScreen(viewModel, shopTitle, onBack)
    }
}
