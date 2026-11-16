package com.coffeepeek.feature.shopreport.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.ui.ShopReportViewModel
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportAction
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Presentation snapshot for native consumers. Repository and domain types stay behind this owner. */
data class NativeShopReportState(
    val selectedCategory: String?,
    val description: String,
    val isSubmitting: Boolean,
    val isSubmitted: Boolean,
    val deliveryUnconfirmed: Boolean,
    val error: String?,
)

/** Owns the existing form ViewModel until the native destination is removed. */
class NativeShopReport internal constructor(shopId: String, repository: ShopIssueReportRepository) {
    private val store = ViewModelStore()
    private val viewModel = ShopReportViewModel(shopId, repository).also { store.put("report", it) }
    private var closed = false
    val states: Flow<NativeShopReportState> = viewModel.state.map { it.nativeSnapshot() }

    fun currentState(): NativeShopReportState = viewModel.state.value.nativeSnapshot()

    fun selectCategory(category: String) {
        if (closed) return
        val selected = ShopIssueCategory.entries.firstOrNull { it.name == category } ?: return
        viewModel.onAction(ShopReportAction.SelectCategory(selected))
    }

    fun changeDescription(description: String) {
        if (!closed) viewModel.onAction(ShopReportAction.ChangeDescription(description))
    }

    fun submit() {
        if (!closed) viewModel.onAction(ShopReportAction.Submit)
    }

    fun close() {
        if (closed) return
        closed = true
        store.clear()
    }
}

fun createNativeShopReport(shopId: String, repository: ShopIssueReportRepository): NativeShopReport =
    NativeShopReport(shopId, repository)

private fun ShopReportState.nativeSnapshot() = NativeShopReportState(
    selectedCategory = selectedCategory?.name,
    description = description,
    isSubmitting = isSubmitting,
    isSubmitted = isSubmitted,
    deliveryUnconfirmed = deliveryUnconfirmed,
    error = error?.name,
)
