package com.coffeepeek.feature.checkinhistory.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.checkinhistory.data.backend.CheckInHistoryBackend
import com.coffeepeek.feature.checkinhistory.data.backend.HistoryPageDto
import com.coffeepeek.feature.checkinhistory.data.mapper.historyIntOrNull
import com.coffeepeek.feature.checkinhistory.data.mapper.toDomain
import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import io.ktor.client.HttpClient
import io.ktor.http.Url

/** The caller supplies/owns the authenticated client and revokes access on account replacement. */
fun createCheckInHistoryRepository(client: HttpClient, apiBaseUrl: String): CheckInHistoryRepository {
    require(apiBaseUrl.startsWith("https://") || apiBaseUrl.startsWith("http://"))
    val base = Url(apiBaseUrl)
    require(base.protocol.name in setOf("http", "https") && base.host.isNotBlank())
    return RemoteCheckInHistoryRepository(CheckInHistoryBackend(client), apiBaseUrl)
}

private class RemoteCheckInHistoryRepository(
    private val backend: CheckInHistoryBackend,
    private val apiBaseUrl: String,
) : CheckInHistoryRepository {
    override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> = requestResult {
        val number = page.coerceAtLeast(1)
        val size = pageSize.coerceIn(1, 100)
        backend.page(number, size).getOrThrow().toDomain(number, size)
    }

    override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> = requestResult {
        val size = 100
        val first = backend.page(1, size, period).getOrThrow().toDomain(1, size)
        val items = first.items.toMutableList()
        check(first.items.isNotEmpty() || first.totalCount == 0) { "Missing first calendar page" }
        for (number in 2..first.totalPages) {
            val next = backend.page(number, size, period).getOrThrow().toDomain(number, size)
            check(next.items.isNotEmpty()) { "Missing calendar page $number" }
            items += next.items
        }
        items.distinctBy(PersonalCheckIn::id)
    }

    private fun HistoryPageDto.toDomain(number: Int, size: Int): CheckInHistoryPage {
        val count = checkNotNull(totalCount.historyIntOrNull()) { "Invalid history total count" }
        require(count >= 0) { "Negative history total count" }
        return CheckInHistoryPage(items.map { it.toDomain(apiBaseUrl) }, count, number, size)
    }
}
