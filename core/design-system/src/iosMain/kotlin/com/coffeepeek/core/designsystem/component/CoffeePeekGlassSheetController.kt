@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.UIVisualEffectView
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

/** One native material behind the transparent sheet's Compose content. */
internal class CoffeePeekGlassSheetController(private val content: UIViewController) : UIViewController(null, null) {
    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.clearColor
        val effect = if (coffeePeekLiquidGlassAvailable) UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular)
            else UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular)
        val glass = UIVisualEffectView(effect).apply {
            setFrame(view.bounds)
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        }
        view.addSubview(glass)
        addChildViewController(content)
        content.view.setFrame(glass.contentView.bounds)
        content.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        content.view.backgroundColor = UIColor.clearColor
        glass.contentView.addSubview(content.view)
        content.didMoveToParentViewController(this)
    }
}
