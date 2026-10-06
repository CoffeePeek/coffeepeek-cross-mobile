package com.coffeepeek.admin.di

import com.coffeepeek.admin.feature.appupdate.data.StoreUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.domain.UpdateInstaller
import org.koin.dsl.module

internal actual fun updateInstallerModule() = module {
    single<UpdateInstaller> { StoreUpdateInstaller() }
}
