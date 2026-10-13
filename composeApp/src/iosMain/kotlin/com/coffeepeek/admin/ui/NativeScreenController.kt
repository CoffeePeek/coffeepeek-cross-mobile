@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.ui.component.NavigationBarAction
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject

/** Owns UIKit chrome and one retained Compose screen during the UI migration. */
internal class NativeScreenController(
    title: String? = null,
    private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {
    var usesNavigationBar = false
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
                SideEffect {
                    overrideUserInterfaceStyle = if (colors.background.luminance() < .5f) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    view.backgroundColor = UIColor.colorWithRed(colors.background.red.toDouble(), colors.background.green.toDouble(), colors.background.blue.toDouble(), 1.0)
                    navigationController?.navigationBar?.tintColor = UIColor.colorWithRed(colors.primary.red.toDouble(), colors.primary.green.toDouble(), colors.primary.blue.toDouble(), 1.0)
                }
                ProvideFloatingNavClearance(0.dp) {
                    Box(Modifier.fillMaxSize().dismissKeyboardOnBackgroundGestures()) { content() }
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
            child.view.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor),
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
