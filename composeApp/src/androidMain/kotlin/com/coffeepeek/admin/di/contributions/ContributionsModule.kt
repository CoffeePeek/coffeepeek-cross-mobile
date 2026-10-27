package com.coffeepeek.admin.di.contributions

import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.contributions.data.repository.createContributionsRepository
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import java.time.OffsetDateTime
import org.koin.dsl.module

internal fun contributionsModule() = module {
    single<ContributionsRepository> {
        val client = get<CoffeePeekClient>()
        createContributionsRepository(client.client, client.uploadClient) { OffsetDateTime.now().offset.totalSeconds / 60 }
    }
}
