@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.community

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.session.SessionReader
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var sheetFactory: (IosCommunitySheet) -> UIViewController
internal fun configureNativeCommunitySheets(factory: (IosCommunitySheet) -> UIViewController) { sheetFactory = factory }

internal actual fun nativeCommunityEditorRenderer(): NativeCommunityEditorRenderer? = { data, actions ->
    NativeCommunitySheet(data.nativeJson(), data.busy, actions::perform)
}

internal actual fun nativeCommunityReportRenderer(): NativeCommunityReportRenderer? = { data, actions ->
    NativeCommunitySheet(data.nativeJson(), data.busy, actions::perform)
}

/** The surrounding design-system sheet retains its analytics overlay and UIKit dismissal delegate. */
@Composable
private fun NativeCommunitySheet(json: String, busy: Boolean, actions: (String, String) -> Unit) {
    val accounts = koinInject<SessionReader>()
    val sheet = remember { IosCommunitySheet(json, busy, actions, accounts) }
    val controller = remember(sheet) { sheetFactory(sheet) }
    val height by sheet.height.collectAsState()
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { sheet.update(json, busy, actions); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(sheet) { onDispose { sheet.close() } }
    NativeEditorControllerHost(controller, Modifier.fillMaxWidth().height(height.toFloat().dp))
}
