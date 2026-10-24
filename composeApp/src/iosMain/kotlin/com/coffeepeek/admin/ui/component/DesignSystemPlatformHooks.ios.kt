package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardOptions
import com.coffeepeek.core.designsystem.component.CoffeePeekHaptic
import com.coffeepeek.core.designsystem.component.CoffeePeekInputRole
import com.coffeepeek.core.designsystem.component.CoffeePeekKeyboardPresenter
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekKeyboardPresenter
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeAlertPresenter
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeDatePickerPresenter
import com.coffeepeek.core.designsystem.component.CoffeePeekNativePhotoSourcePresenter
import com.coffeepeek.core.designsystem.component.CoffeePeekPlatformEffects
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekNativeAlertPresenter
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekNativeDatePickerPresenter
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekNativePhotoSourcePresenter
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekPlatformEffects
import com.coffeepeek.core.designsystem.component.CoffeePeekHeaderAction
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeHeaderPresenter
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeSheetPresenter
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekNativeHeaderPresenter
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekNativeSheetPresenter

/** The UIKit page sheet of [NativeModalSheet]; iOS before 16 reports false and the design-system sheet is used. */
private object IosNativeSheetPresenter : CoffeePeekNativeSheetPresenter {
    @Composable
    override fun Present(
        onDismiss: () -> Unit,
        dismissEnabled: Boolean,
        maxSheetHeightFraction: Float?,
        content: @Composable ColumnScope.() -> Unit,
    ): Boolean {
        val tracked: @Composable ColumnScope.() -> Unit = {
            val column = this
            AnalyticsOverlay { content(column) }
        }
        var handled = false
        CompositionLocalProvider(LocalNativeSheetHeightFraction provides maxSheetHeightFraction) {
            handled = NativeModalSheet(onDismiss, dismissEnabled, tracked)
        }
        return handled
    }
}

/** The title, back and action of the pushed controller's navigation item ([NativeNavigationBar]). */
private object IosNativeHeaderPresenter : CoffeePeekNativeHeaderPresenter {
    @Composable
    override fun Present(title: String, onBack: (() -> Unit)?, customBack: Boolean, action: CoffeePeekHeaderAction?): Boolean =
        NativeNavigationBar(title, onBack, customBack, action?.let { NavigationBarAction(it.label, it.onClick) })
}

/** The UIDatePicker wheels of [NativeDatePicker]. */
private object IosNativeDatePickerPresenter : CoffeePeekNativeDatePickerPresenter {
    @Composable
    override fun Present(
        title: String, initialMillis: Long, maximumMillis: Long?, timeOnly: Boolean,
        onConfirm: (Long) -> Unit, onDismiss: () -> Unit,
    ): Boolean = NativeDatePicker(title, initialMillis, maximumMillis, timeOnly, onConfirm, onDismiss)
}

/** The system alert of [NativeAlert]. */
private object IosNativeAlertPresenter : CoffeePeekNativeAlertPresenter {
    @Composable
    override fun Present(
        title: String, message: String, confirmLabel: String, onConfirm: () -> Unit,
        onDismiss: () -> Unit, cancelLabel: String?, destructive: Boolean,
    ): Boolean = NativeAlert(title, message, confirmLabel, onConfirm, onDismiss, cancelLabel, destructive)
}

/** The UIAlertController action sheet of [NativePhotoSourceSheet]. */
private object IosNativePhotoSourcePresenter : CoffeePeekNativePhotoSourcePresenter {
    @Composable
    override fun Present(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit): Boolean =
        NativePhotoSourceSheet(title, onGallery, onCamera, onDismiss)
}

/** Haptics and the status-bar scroll-to-top bridge of the legacy components. */
private object IosPlatformEffects : CoffeePeekPlatformEffects {
    override fun haptic(event: CoffeePeekHaptic) = platformHaptic(
        when (event) {
            CoffeePeekHaptic.Selection -> HapticEvent.Selection
            CoffeePeekHaptic.Success -> HapticEvent.Success
            CoffeePeekHaptic.Error -> HapticEvent.Error
        },
    )

    @Composable
    override fun ScrollToTop(state: LazyListState) = PlatformScrollToTop(state)
}

/** UIKit traits, AutoFill content types and the keyboard "Done" bar of the legacy fields. */
private object IosKeyboardPresenter : CoffeePeekKeyboardPresenter {
    override fun textInputOptions(options: KeyboardOptions, role: CoffeePeekInputRole): KeyboardOptions = when (role) {
        CoffeePeekInputRole.Default -> platformTextInputOptions(options)
        CoffeePeekInputRole.Credential -> platformAuthKeyboardOptions(options, newPassword = false)
        CoffeePeekInputRole.NewPassword -> platformAuthKeyboardOptions(options, newPassword = true)
    }

    @Composable
    override fun DoneAccessory(visible: Boolean, onDone: () -> Unit) = NativeKeyboardDoneAccessory(visible, onDone)
}

@Composable
internal actual fun ProvideDesignSystemPlatformHooks(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCoffeePeekNativeSheetPresenter provides IosNativeSheetPresenter,
        LocalCoffeePeekNativeHeaderPresenter provides IosNativeHeaderPresenter,
        LocalCoffeePeekNativeDatePickerPresenter provides IosNativeDatePickerPresenter,
        LocalCoffeePeekNativeAlertPresenter provides IosNativeAlertPresenter,
        LocalCoffeePeekNativePhotoSourcePresenter provides IosNativePhotoSourcePresenter,
        LocalCoffeePeekPlatformEffects provides IosPlatformEffects,
        LocalCoffeePeekKeyboardPresenter provides IosKeyboardPresenter,
        content = content,
    )
}
