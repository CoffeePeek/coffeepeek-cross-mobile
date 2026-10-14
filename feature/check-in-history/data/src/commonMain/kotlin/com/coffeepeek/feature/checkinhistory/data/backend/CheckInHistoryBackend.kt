package com.coffeepeek.feature.checkinhistory.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

internal class CheckInHistoryBackend(private val client: HttpClient) {
    suspend fun page(number: Int, size: Int, period: HistoryPeriod? = null): Result<HistoryPageDto> = requestResult {
        val response = client.get("/api/v1/check-ins/mine") {
            expectSuccess = false
            parameter("pageNumber", number)
            parameter("pageSize", size)
            period?.let {
                parameter("from", it.fromInclusive)
                parameter("to", it.toExclusive)
            }
        }
        check(response.status.isSuccess()) { "Personal history request failed (${response.status.value})" }
        val envelope = response.body<HistoryEnvelope>()
        check(envelope.isSuccess) { "Personal history request was rejected" }
        checkNotNull(envelope.data) { "Missing personal history data" }
    }
}
