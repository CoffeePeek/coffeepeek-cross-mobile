package com.coffeepeek.admin.sandbox.backend

import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.*
import java.time.LocalDate

private const val SELF_INDEX = 39

internal fun sandboxLeaderboard(request: HttpRequestData, today: LocalDate, populated: Boolean,
    username: String, friendVisible: Boolean): Pair<HttpStatusCode, JsonObject> {
    val period = request.url.parameters["period"] ?: "week"
    val page = request.url.parameters["page"]?.toIntOrNull() ?: 1
    val size = request.url.parameters["pageSize"]?.toIntOrNull() ?: 20
    val date = runCatching { request.url.parameters["date"]?.let(LocalDate::parse) ?: today }.getOrNull()
    if (period !in listOf("week", "month") || page !in 1..10_000 || size !in 1..100 || date == null) {
        return HttpStatusCode.BadRequest to buildJsonObject { put("isSuccess", false) }
    }
    val start = if (period == "month") date.withDayOfMonth(1) else date.minusDays(date.dayOfWeek.value - 1L)
    val rows = sandboxLeaderboardRows(populated, username, friendVisible, period == "month")
    return HttpStatusCode.OK to buildJsonObject {
        put("isSuccess", true)
        put("data", sandboxLeaderboardPage(request, period, start, rows, page, size))
    }
}

private fun sandboxLeaderboardPage(request: HttpRequestData, period: String, start: LocalDate,
    rows: List<JsonObject>, page: Int, size: Int) = buildJsonObject {
    put("period", period)
    put("startsOn", start.toString())
    put("endsOnExclusive", (if (period == "month") start.plusMonths(1) else start.plusWeeks(1)).toString())
    put("timeZone", "Europe/Minsk")
    put("totalCount", rows.size)
    put("page", page)
    put("pageSize", size)
    put("items", JsonArray(rows.drop((page - 1) * size).take(size)))
    put("me", if (request.headers[HttpHeaders.Authorization] == null) JsonNull else rows.lastOrNull() ?: JsonNull)
}

private fun sandboxLeaderboardRows(populated: Boolean, username: String, friendVisible: Boolean, month: Boolean): List<JsonObject> {
    if (!populated) return emptyList()
    val indices = (0..SELF_INDEX).filter { friendVisible || it != 0 }
    return indices.map { index ->
        val score = sandboxLeaderboardScore(index, month)
        buildJsonObject {
            put("rank", indices.count { sandboxLeaderboardScore(it, month) > score } + 1)
            putLeaderboardIdentity(index, username)
            put("avatarUrl", JsonNull)
            put("points", score)
            put("additions", 0)
            put("edits", 0)
            put("checkIns", score / 2)
        }
    }
}

private fun JsonObjectBuilder.putLeaderboardIdentity(index: Int, username: String) {
    put("authorId", when (index) {
        0 -> SandboxApplicationBackend.FRIEND_ID
        SELF_INDEX -> SandboxApplicationBackend.SELF_ID
        else -> "sandbox-leader-$index"
    })
    put("username", when (index) {
        0 -> SandboxApplicationBackend.FRIEND_NAME
        SELF_INDEX -> username
        else -> "Участник ${index + 1}"
    })
    if (index == 0 || index == SELF_INDEX) putJsonObject("address") {
        put("slug", if (index == 0) SandboxApplicationBackend.FRIEND_SLUG else "sandbox")
    }
}

private fun sandboxLeaderboardScore(index: Int, month: Boolean): Int =
    (if (index < 2) 100 else 100 - index * 2) * (if (month) 2 else 1)
