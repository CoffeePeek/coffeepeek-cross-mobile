package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** The selected day's diary needs every Home row, not just the first page of a month/week. */
internal suspend fun readActivityHomeRows(reader: HomeHistoryReader, range: HomeHistoryDateRange): List<HomeHistoryEntry> {
    val entries = linkedMapOf<String, HomeHistoryEntry>()
    var number = 1
    do {
        currentCoroutineContext().ensureActive()
        val page = reader.page(number, 100, range).getOrThrow()
        currentCoroutineContext().ensureActive()
        check(page.page == number && page.pageSize == 100 && (page.items.isNotEmpty() || !page.hasMore))
        page.items.filter { it.preparedOn in range.from..range.to }.forEach { if (it.id !in entries) entries[it.id] = it }
        if (!page.hasMore) break
        check(number < 10000)
        number++
    } while (true)
    return entries.values.toList()
}
