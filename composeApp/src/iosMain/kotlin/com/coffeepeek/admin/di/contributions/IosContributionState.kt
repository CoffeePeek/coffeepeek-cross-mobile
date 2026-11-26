package com.coffeepeek.admin.di.contributions

import com.coffeepeek.feature.contributions.impl.NativeChangeRequestState
import com.coffeepeek.feature.contributions.impl.NativeContributionField
import com.coffeepeek.feature.contributions.impl.NativeContributionItem
import com.coffeepeek.feature.contributions.impl.NativeContributionText
import com.coffeepeek.feature.contributions.impl.NativeContributionsListState

data class IosContributionsListState(
    val loading: Boolean, val error: String?, val serverMessage: String?,
    val tabs: List<IosContributionTab>, val selected: String?, val items: List<IosContributionItem>,
    val hasMore: Boolean, val loadingMore: Boolean, val moreFailed: Boolean,
)

data class IosContributionTab(val status: String, val count: Int)

data class IosContributionItem(
    val id: String, val title: String?, val subtitle: String?, val section: String?,
    val summary: IosContributionText?, val date: String?, val waiting: Boolean, val reason: String?, val canOpen: Boolean,
)

data class IosContributionText(val key: String, val count: Int, val extra: Int)

data class IosChangeRequestState(
    val loading: Boolean, val error: String?, val serverMessage: String?,
    val section: String?, val status: String?, val reason: String?, val date: String?,
    val fields: List<IosContributionField>, val note: String?, val canOpenShop: Boolean,
)

data class IosContributionField(
    val labelKey: String?, val label: String?, val valueKey: String?, val value: String?, val price: String?,
)

internal fun NativeContributionsListState.ios() = IosContributionsListState(
    loading, error, serverMessage, tabs.map { IosContributionTab(it.status, it.count) }, selected,
    items.map(NativeContributionItem::ios), hasMore, loadingMore, moreFailed,
)

internal fun NativeChangeRequestState.ios() = IosChangeRequestState(
    loading, error, serverMessage, section, status, reason, date, fields.map(NativeContributionField::ios), note, canOpenShop,
)

private fun NativeContributionItem.ios() = IosContributionItem(
    id, title, subtitle, section, summary?.ios(), date, waiting, reason, canOpen,
)

private fun NativeContributionText.ios() = IosContributionText(key, count, extra)

private fun NativeContributionField.ios() = IosContributionField(labelKey, label, valueKey, value, price)
