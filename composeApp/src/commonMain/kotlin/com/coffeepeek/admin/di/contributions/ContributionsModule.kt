package com.coffeepeek.admin.di.contributions

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.data.repository.createContributionCatalogSource
import com.coffeepeek.feature.contributions.data.repository.createContributionsRepository
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import org.koin.dsl.module

internal fun contributionsModule() = module {
    single<ContributionsRepository> {
        val client = get<ApiClients>()
        createContributionsRepository(client.client, client.uploadClient, ::contributionUtcOffsetMinutes)
    }
    single<ContributionCatalogs> {
        SourceContributionCatalogs(createContributionCatalogSource(get<ApiClients>().client, roasterOptions(get())))
    }
}
