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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import com.coffeepeek.core.designsystem.component.CoffeePeekScreenInsets
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekScreenInsets
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.modifier.CoffeePeekNativeGlassPresenter
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekNativeGlassPresenter
import kotlinx.cinterop.useContents
import platform.UIKit.UINavigationController
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
    private var bottomClearance by mutableStateOf(if (isTabRoot) CpDimens.iosTabBarClearanceFloor else 0.dp)
    private var tabBarHidden = false
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
        syncBottomClearance()
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
        refreshBottomClearance()
    }

    internal fun syncBottomClearance(hidden: Boolean = tabBarHidden) {
        tabBarHidden = hidden
        refreshBottomClearance()
    }

    private fun refreshBottomClearance() {
        if (isTabRoot && !isVisibleTabRoot()) return
        bottomClearance = measuredBottomClearance()
    }

    /** Tab root on screen with tab bar shown; not the controller left mounted under a push. */
    private fun isVisibleTabRoot(): Boolean {
        val navigation = sequence {
            var parent = parentViewController
            while (parent != null) {
                yield(parent)
                parent = parent.parentViewController
            }
        }.filterIsInstance<UINavigationController>().firstOrNull()
        return navigation?.topViewController === this || navigation == null
    }

    private fun measuredBottomClearance(): Dp {
        val inset = view.safeAreaInsets.useContents { bottom }.dp
        if (!isTabRoot || tabBarHidden) return inset
        return maxOf(inset, CpDimens.iosTabBarClearanceFloor)
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
            view.tintColor = colors.primary.toUIColor()
            setNeedsStatusBarAppearanceUpdate()
        }
        val floatingClearance = if (isTabRoot) bottomClearance else 0.dp
        ProvideFloatingNavClearance(floatingClearance) {
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
                    LocalCoffeePeekNativeGlassPresenter provides IosCoffeePeekGlassPresenter,
                    LocalCoffeePeekScreenInsets provides CoffeePeekScreenInsets(
                        bottom = if (isTabRoot || isAuthFlow) 0.dp else bottomClearance,
                        extendsToBottom = !isTabRoot && !isAuthFlow,
                    ),
                ) {
                    content()
                }
            }
        }
    }
}

private object IosCoffeePeekGlassPresenter : CoffeePeekNativeGlassPresenter {
    @Composable
    override fun IconButton(icon: CoffeePeekGlassIcon, onClick: () -> Unit, contentDescription: String,
        modifier: Modifier, enabled: Boolean, selected: Boolean, isLoading: Boolean): Boolean {
        val platformIcon = when (icon) {
            CoffeePeekGlassIcon.Back -> GlassControlIcon.Back
            CoffeePeekGlassIcon.Add -> GlassControlIcon.Add
            CoffeePeekGlassIcon.Edit -> GlassControlIcon.Edit
            CoffeePeekGlassIcon.Favorite -> GlassControlIcon.Favorite
            CoffeePeekGlassIcon.FavoriteFilled -> GlassControlIcon.FavoriteFilled
            CoffeePeekGlassIcon.Share -> GlassControlIcon.Share
            CoffeePeekGlassIcon.Close -> GlassControlIcon.Close
        }
        PlatformGlassIconButton(platformIcon, onClick, contentDescription, modifier, enabled, selected, isLoading) {}
        return true
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
