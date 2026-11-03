package com.coffeepeek.admin.sandbox.di

import com.coffeepeek.admin.di.collections.CollectionsHttpClient
import com.coffeepeek.core.network.ApiClients
import org.koin.dsl.module

/** Sandbox deliberately has no bearer credentials; its local engine still uses the active fake owner. */
internal fun sandboxCollectionsModule() = module {
    single { CollectionsHttpClient(get<ApiClients>().client, get(), verifyBearer = false) }
}
