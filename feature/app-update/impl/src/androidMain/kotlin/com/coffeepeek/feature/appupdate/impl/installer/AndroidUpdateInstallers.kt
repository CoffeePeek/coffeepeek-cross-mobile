package com.coffeepeek.feature.appupdate.impl.installer

import android.content.Context
import com.coffeepeek.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment

/** The APK channel downloads and installs the package itself; every other channel delegates to Google Play. */
fun createAndroidUpdateInstaller(context: Context, environment: AppUpdateEnvironment): AndroidUpdateInstaller =
    if (environment.channel == "apk") ApkUpdateInstaller(context) else PlayUpdateInstaller(context, environment)
