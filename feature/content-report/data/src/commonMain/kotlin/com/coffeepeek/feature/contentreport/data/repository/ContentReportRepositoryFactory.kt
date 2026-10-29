package com.coffeepeek.feature.contentreport.data.repository

import com.coffeepeek.feature.contentreport.data.backend.ContentReportBackend
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import io.ktor.client.HttpClient

/** Uses the existing authenticated client; no transport, session store or SDK is created here. */
fun createContentReportRepository(client: HttpClient): ContentReportRepository =
    ContentReportBackend(client).let { backend -> ContentReportRepository(backend::submit) }
