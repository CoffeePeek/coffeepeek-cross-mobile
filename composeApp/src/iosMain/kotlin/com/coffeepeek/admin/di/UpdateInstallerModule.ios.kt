package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.appupdate.StoreUpdateInstaller
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import org.koin.dsl.module

internal actual fun updateInstallerModule() = module {
    single<UpdateInstaller> { StoreUpdateInstaller() }
}
