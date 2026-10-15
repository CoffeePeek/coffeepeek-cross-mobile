package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

internal enum class HistoryKindFilter {
    All, Shop, Home;

    fun includes(source: HistorySource): Boolean = when (this) {
        All -> true
        Shop -> source == HistorySource.Shop
        Home -> source == HistorySource.Home
    }
}

internal enum class HistorySource { Shop, Home }
