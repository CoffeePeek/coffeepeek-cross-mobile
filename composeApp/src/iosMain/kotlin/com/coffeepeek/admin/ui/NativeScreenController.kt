@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.ui.component.NavigationBarAction
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.theme.ThemeManager
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject

/** Owns UIKit chrome and one retained Compose screen during the UI migration. */
internal class NativeScreenController(
    title: String? = null,
    val route: Navigator.Screen? = null,
    private val extendsUnderHomeIndicator: Boolean = false,
    private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {
    var usesNavigationBar by mutableStateOf(false)
        private set
    var handlesBackLocally = false
        private set
    private val backTarget = BarActionTarget()
    private val actionTarget = BarActionTarget()

    init {
        this.title = title
        edgesForExtendedLayout = UIRectEdgeNone
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        val child = ComposeUIViewController {
            AppContent {
                val colors = MaterialTheme.colorScheme
                val themeMode by ThemeManager.themeMode.collectAsState()
                SideEffect {
                    overrideUserInterfaceStyle = themeMode.uiKitStyle
                    view.backgroundColor = colors.background.toUIColor()
                    // UIKit reserves the header's space, so it has no Compose content behind it.
                    val appearance = UINavigationBarAppearance().apply {
                        configureWithOpaqueBackground()
                        backgroundColor = colors.background.toUIColor()
                        titleTextAttributes = mapOf(NSForegroundColorAttributeName to colors.onBackground.toUIColor())
                    }
                    navigationItem.standardAppearance = appearance
                    navigationItem.scrollEdgeAppearance = appearance
                    navigationItem.compactAppearance = appearance
                    navigationController?.navigationBar?.tintColor = colors.primary.toUIColor()
                }
                ProvideFloatingNavClearance(0.dp) {
                    // The child is bounded above the tab bar/home indicator. When a native
                    // header is visible, UIKit also bounds it below the navigation bar.
                    val reservedSides = if (usesNavigationBar) WindowInsetsSides.Top + WindowInsetsSides.Bottom
                        else WindowInsetsSides.Bottom
                    Box(Modifier.fillMaxSize()
                        .background(colors.background)
                        .then(if (extendsUnderHomeIndicator) Modifier else Modifier.consumeWindowInsets(WindowInsets.systemBars.only(reservedSides)))
                        .dismissKeyboardOnBackgroundGestures()) { content() }
                }
            }
        }
        addChildViewController(child)
        child.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(child.view)
        NSLayoutConstraint.activateConstraints(listOf(
            child.view.topAnchor.constraintEqualToAnchor(view.topAnchor),
            child.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            child.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
            child.view.bottomAnchor.constraintEqualToAnchor(
                if (extendsUnderHomeIndicator) view.bottomAnchor else view.safeAreaLayoutGuide.bottomAnchor,
            ),
        ))
        child.didMoveToParentViewController(this)
    }

    fun configureHeader(title: String, back: (() -> Unit)?, customBack: Boolean, action: NavigationBarAction?) {
        usesNavigationBar = true
        handlesBackLocally = customBack && back != null
        navigationItem.title = title
        navigationItem.setHidesBackButton(back == null || handlesBackLocally, false)
        backTarget.callback = back
        navigationItem.leftBarButtonItem = if (handlesBackLocally) UIBarButtonItem(
            title = "Назад", style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
            target = backTarget, action = NSSelectorFromString("invoke"),
        ) else null
        actionTarget.callback = action?.onClick
        navigationItem.rightBarButtonItem = action?.let {
            UIBarButtonItem(title = it.label, style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                target = actionTarget, action = NSSelectorFromString("invoke"))
        }
        if (navigationController?.topViewController == this) navigationController?.setNavigationBarHidden(false, false)
    }

    fun clearHeader() {
        usesNavigationBar = false
        handlesBackLocally = false
        backTarget.callback = null
        actionTarget.callback = null
        navigationItem.leftBarButtonItem = null
        navigationItem.rightBarButtonItem = null
        if (navigationController?.topViewController == this) navigationController?.setNavigationBarHidden(true, false)
    }
}

private class BarActionTarget : NSObject() {
    var callback: (() -> Unit)? = null
    @ObjCAction fun invoke() { callback?.invoke() }
}

/** Sheets cannot change their presenting screen's navigation bar. */
internal fun UIViewController.nativeScreenHost(): NativeScreenController? {
    var current: UIViewController? = this
    while (current != null) {
        if (current is NativeScreenController) return current
        current = current.parentViewController
    }
    return null
}
