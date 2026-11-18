@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.coffeepeek.admin.di.system.IosRootModal
import com.coffeepeek.admin.di.system.IosRootModalObservation
import com.coffeepeek.admin.di.system.NativeRootModal
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.homecheckin.impl.guestCheckInOnboardingPoster
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

internal val nativeGuestOnboardingVisible = MutableStateFlow(false)

@Composable
internal actual fun PlatformGuestCheckInOnboarding(onFinish: (Navigator.Screen?) -> Unit): Boolean {
    val language = koinInject<LanguagePort>()
    val finish = rememberUpdatedState(onFinish)
    val model = remember(language) { IosGuestOnboarding(language) { finish.value(it) } }
    DisposableEffect(model) {
        nativeGuestOnboardingVisible.value = true
        onDispose { nativeGuestOnboardingVisible.value = false }
    }
    NativeRootModal(model)
    return true
}

class IosGuestOnboarding internal constructor(language: LanguagePort, private val finish: (Navigator.Screen?) -> Unit) : IosRootModal(language) {
    private var finished = false
    fun continueAsGuest() = complete(null)
    fun signIn() = complete(Navigator.Screen.Auth)
    fun register() = complete(Navigator.Screen.Register)
    fun loadPoster(index: Int, receive: (UIImage?) -> Unit) = IosRootModalObservation(scope.launch {
        val bytes = guestCheckInOnboardingPoster(index)
        receive(if (bytes.isEmpty()) null else bytes.usePinned { UIImage.imageWithData(NSData.create(it.addressOf(0), bytes.size.toULong())) })
    })
    private fun complete(route: Navigator.Screen?) {
        if (finished || !isActive) return
        finished = true
        finish(route)
    }
}
