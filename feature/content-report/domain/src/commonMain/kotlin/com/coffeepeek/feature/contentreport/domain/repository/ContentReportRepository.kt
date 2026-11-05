package com.coffeepeek.feature.contentreport.domain.repository

import com.coffeepeek.feature.contentreport.domain.model.ContentReportTarget

fun interface ContentReportRepository {
    suspend fun submit(target: ContentReportTarget, reason: String): Result<Unit>
}
