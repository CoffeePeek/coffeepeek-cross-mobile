package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.coffeepeek.admin.analytics.AnalyticsOverlay
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

@Composable
internal actual fun ProvideDesignSystemPlatformHooks(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCoffeePeekNativeSheetPresenter provides IosNativeSheetPresenter,
        LocalCoffeePeekNativeHeaderPresenter provides IosNativeHeaderPresenter,
        content = content,
    )
}
