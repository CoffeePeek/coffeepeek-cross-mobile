package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.contentreport.data.repository.createContentReportRepository
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import org.koin.dsl.module

internal fun contentReportModule() = module {
    single<ContentReportRepository> { createContentReportRepository(get<CoffeePeekClient>().client) }
    single { FeatureContentReportRenderer(get(), get()) }
}
