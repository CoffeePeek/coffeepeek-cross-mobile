@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.analytics.TrackNativeFeatureExposure
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.PresentNativeController
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import platform.UIKit.UIViewController

class IosCheckInCreatePicker internal constructor(
    val languageTag: String,
    private val actions: () -> CheckInCreatePickerActions,
    private val onVisibility: (Boolean) -> Unit,
) {
    fun dismiss() = actions().onDismiss()
    fun chooseShop() = actions().onShop()
    fun chooseHome() = actions().onHome()
    fun chooseShelfBag() = actions().onShelfBag()
    fun visibilityChanged(visible: Boolean) = onVisibility(visible)
}

internal fun iosCheckInCreatePickerContent(
    factory: (IosCheckInCreatePicker) -> UIViewController,
): @Composable (CheckInCreatePickerActions) -> Unit = { actions ->
    val host = LocalUIViewController.current
    val actionsState = rememberUpdatedState(actions)
    val language = LocalCoffeePeekLanguageTag.current
    val theme by ThemeManager.themeMode.collectAsState()
    val visible = remember(host, factory, language) { mutableStateOf(false) }
    val controller = remember(visible) {
        factory(IosCheckInCreatePicker(language, { actionsState.value }) { visible.value = it })
    }
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    TrackNativeFeatureExposure(visible.value, "home_check_in", "check_in_type_picker")
    PresentNativeController(controller, host)
}
