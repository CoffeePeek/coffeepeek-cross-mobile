package com.coffeepeek.admin.di.roaster

import com.coffeepeek.feature.roaster.impl.NativeRoasterDetailContent
import com.coffeepeek.feature.roaster.impl.NativeRoasterDiscoveryContent

internal expect fun nativeRoasterDiscoveryContent(): NativeRoasterDiscoveryContent?
internal expect fun nativeRoasterDetailContent(): NativeRoasterDetailContent?
