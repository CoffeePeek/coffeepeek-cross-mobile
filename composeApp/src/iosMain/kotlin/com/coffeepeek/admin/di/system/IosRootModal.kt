package com.coffeepeek.admin.di.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.PresentNativeController
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController

private lateinit var nativeRootModal: (IosRootModal) -> UIViewController

internal fun configureNativeRootModals(factory: (IosRootModal) -> UIViewController) { nativeRootModal = factory }

abstract class IosRootModal internal constructor(private val language: LanguagePort) {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var closed = false
    protected val isActive get() = !closed
    fun languageTag() = language.current().languageTag
    fun observeLanguage(receive: (String) -> Unit) = IosRootModalObservation(scope.launch {
        language.observe().collect { receive(it.languageTag) }
    })
    internal fun close() {
        closed = true
        scope.cancel()
    }
}

class IosRootModalObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeRootModal(model: IosRootModal) {
    val host = LocalUIViewController.current
    val controller = remember(model) { nativeRootModal(model) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    PresentNativeController(controller, host)
    DisposableEffect(model) { onDispose { model.close() } }
}
