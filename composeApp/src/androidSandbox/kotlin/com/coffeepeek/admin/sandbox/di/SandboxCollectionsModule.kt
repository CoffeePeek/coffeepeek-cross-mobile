package com.coffeepeek.admin.sandbox.di

import com.coffeepeek.admin.di.collections.CollectionsHttpClient
import com.coffeepeek.admin.di.shelf.ShelfProfileHttpClient
import com.coffeepeek.api.CoffeePeekClient
import org.koin.dsl.module

/** Sandbox deliberately has no bearer credentials; its local engine still uses the active fake owner. */
internal fun sandboxCollectionsModule() = module {
    single { CollectionsHttpClient(get<CoffeePeekClient>().client, get(), verifyBearer = false) }
    single { ShelfProfileHttpClient(get<CoffeePeekClient>().client, get(), verifyBearer = false) }
}
