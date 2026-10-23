package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.installer.createAndroidUpdateInstaller
import org.koin.dsl.module

internal actual fun updateInstallerModule() = module {
    single<AndroidUpdateInstaller> { createAndroidUpdateInstaller(Locator.appContext, AppConfigUpdateEnvironment) }
    single<UpdateInstaller> { get<AndroidUpdateInstaller>() }
}
