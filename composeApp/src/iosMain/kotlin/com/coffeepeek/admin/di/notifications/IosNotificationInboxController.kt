
package com.coffeepeek.admin.di.notifications

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.notifications.NotificationsRenderer
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.impl.createIosNotificationInboxSession
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosNotificationInboxController =
    staticCompositionLocalOf<(IosNotificationInbox, String) -> UIViewController> {
        error("The iOS app must provide a notification inbox controller")
    }

@Composable
internal fun iosNotificationInboxAction(): @Composable () -> Unit {
    val make = iosNotificationInboxFactory()
    val language by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    return { key(language) { NotificationInboxButton(make) } }
}

@Composable
internal fun iosNotificationInboxFactory(): () -> UIViewController {
    val factory = LocalIosNotificationInboxController.current
    val feature = koinInject<NotificationFeature>()
    val renderer = koinInject<NotificationsRenderer>()
    val language by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    return {
        val session = createIosNotificationInboxSession(feature, renderer::open) {
            Navigator.navigate(Navigator.Screen.Auth)
        }
        factory(IosNotificationInbox(session), language)
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
private fun NotificationInboxButton(makeController: () -> UIViewController) {
    UIKitViewController(
        factory = makeController,
        modifier = Modifier.size(48.dp),
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true,
        ),
    )
}
