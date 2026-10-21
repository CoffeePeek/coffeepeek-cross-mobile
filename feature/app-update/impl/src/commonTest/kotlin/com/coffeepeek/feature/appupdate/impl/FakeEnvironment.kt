package com.coffeepeek.feature.appupdate.impl

import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment

internal class FakeEnvironment(override val versionCode: Long?, override val channel: String? = "play") : AppUpdateEnvironment {
    override val platform = "android"
}
