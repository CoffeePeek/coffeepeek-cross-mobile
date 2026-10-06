package com.coffeepeek.admin.di

import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.data.ApkUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.data.PlayUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.admin.locator.Locator
import org.koin.dsl.module

internal actual fun updateInstallerModule() = module {
    single<AndroidUpdateInstaller> {
        if (BuildConfig.APK_UPDATES_ENABLED) ApkUpdateInstaller(Locator.appContext)
        else PlayUpdateInstaller(Locator.appContext)
    }
    single<UpdateInstaller> { get<AndroidUpdateInstaller>() }
}
