@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import com.coffeepeek.core.designsystem.component.CoffeePeekScreenInsets
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekScreenInsets
import kotlinx.cinterop.useContents
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIRectEdgeAll
import platform.UIKit.UIRectEdgeBottom
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

/** UIKit owns navigation and bounds; Compose owns headers and edge-to-edge insets. */
internal class NativeScreenController(
    title: String? = null,
    val route: Navigator.Screen? = null,
    private val analyticsPage: String? = null,
    private val isTabRoot: Boolean = false,
    private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {
    private val analyticsVisit = com.coffeepeek.admin.analytics.correlationId("screen")
    private var analyticsVisible by mutableStateOf(false)
    private var bottomClearance by mutableStateOf(0.dp)
    private val photoUnderStatusBar = route is Navigator.Screen.ShopDetail || route is Navigator.Screen.RoasterDetail
    private val isAuthFlow = route == Navigator.Screen.Auth || route == Navigator.Screen.Register ||
        route == Navigator.Screen.ForgotPassword || route is Navigator.Screen.ResetPassword
    private val extendsUnderStatusBar = photoUnderStatusBar || isAuthFlow
    var handlesBackLocally = false

    init {
        this.title = title
        edgesForExtendedLayout = when {
            extendsUnderStatusBar -> UIRectEdgeAll
            else -> UIRectEdgeBottom
        }
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle =
        if (photoUnderStatusBar) UIStatusBarStyleLightContent else super.preferredStatusBarStyle()

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        analyticsVisible = true
        val page = analyticsPage ?: com.coffeepeek.admin.analytics.analyticsPage(route?.let { it::class.simpleName })
        com.coffeepeek.admin.analytics.Analytics.service?.screenShown(page, analyticsVisit)
    }

    override fun viewWillDisappear(animated: Boolean) {
        analyticsVisible = false
        super.viewWillDisappear(animated)
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.clipsToBounds = true
        attachContent(ComposeUIViewController { AppContent { ScreenContent() } })
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        bottomClearance = view.safeAreaInsets.useContents { bottom }.dp
    }

    private fun attachContent(child: UIViewController) {
        addChildViewController(child)
        child.view.translatesAutoresizingMaskIntoConstraints = false
        child.view.clipsToBounds = true
        view.addSubview(child.view)
        val safeArea = view.safeAreaLayoutGuide
        NSLayoutConstraint.activateConstraints(listOf(
            child.view.topAnchor.constraintEqualToAnchor(if (extendsUnderStatusBar) view.topAnchor else safeArea.topAnchor),
            child.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            child.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
            child.view.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
        ))
        child.didMoveToParentViewController(this)
    }

    @Composable
    private fun ScreenContent() {
        val colors = MaterialTheme.colorScheme
        val themeMode by ThemeManager.themeMode.collectAsState()
        SideEffect {
            overrideUserInterfaceStyle = themeMode.uiKitStyle
            view.backgroundColor = colors.background.toUIColor()
            setNeedsStatusBarAppearanceUpdate()
        }
        ProvideFloatingNavClearance(if (isTabRoot) bottomClearance else 0.dp) {
            // Tab content scrolls behind the glass; lists and controls reserve the measured safe area.
            Box(Modifier.fillMaxSize().background(colors.background)
                .consumeWindowInsets(when {
                    extendsUnderStatusBar -> WindowInsets(0, 0, 0, 0)
                    isTabRoot -> WindowInsets.systemBars.only(WindowInsetsSides.Vertical)
                    else -> WindowInsets.systemBars.only(WindowInsetsSides.Top)
                })
                .dismissKeyboardOnBackgroundGestures()) {
                CompositionLocalProvider(
                    com.coffeepeek.admin.analytics.LocalAnalyticsVisible provides analyticsVisible,
                    LocalCoffeePeekScreenInsets provides CoffeePeekScreenInsets(
                        bottom = if (isTabRoot || isAuthFlow) 0.dp else bottomClearance,
                        extendsToBottom = !isTabRoot && !isAuthFlow,
                    ),
                ) {
                    content()
                    // Native tabs keep multiple controllers alive; only the visible one presents prompts.
                    if (analyticsVisible) com.coffeepeek.admin.di.authorblocking.AuthorBlockingRootHost()
                }
            }
        }
    }
}

/** Sheets cannot change their presenting screen's back policy. */
internal fun UIViewController.nativeScreenHost(): NativeScreenController? {
    var current: UIViewController? = this
    while (current != null) {
        if (current is NativeScreenController) return current
        current = current.parentViewController
    }
    return null
}
