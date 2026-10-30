package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.contentreport.data.repository.createContentReportRepository
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import org.koin.dsl.module

internal fun contentReportModule() = module {
    single<ContentReportRepository> { createContentReportRepository(get<ApiClients>().client) }
    single { FeatureContentReportRenderer(get(), get()) }
}
