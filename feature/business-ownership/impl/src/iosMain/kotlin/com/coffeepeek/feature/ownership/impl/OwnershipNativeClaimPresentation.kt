@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.feature.ownership.impl

import androidx.compose.runtime.*
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipAction
import platform.UIKit.*
import platform.darwin.NSObject

/** Claim actions are embedded in a small toolbar slot; their form must be presented as a real native sheet. */
@Composable
internal fun OwnershipNativeClaimPresentation(controller: UIViewController, model: OwnershipViewModel) {
    val host = LocalUIViewController.current
    val delegate = remember(model) { OwnershipClaimDismiss { model.onAction(OwnershipAction.Back) } }
    DisposableEffect(controller) {
        controller.modalPresentationStyle = UIModalPresentationPageSheet
        controller.modalInPresentation = true
        host.presentViewController(controller, animated = true) { controller.presentationController?.delegate = delegate }
        onDispose { controller.dismissViewControllerAnimated(true, completion = null) }
    }
}

private class OwnershipClaimDismiss(private val back: () -> Unit) : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
    override fun presentationControllerShouldDismiss(presentationController: UIPresentationController): Boolean = false
    override fun presentationControllerDidAttemptToDismiss(presentationController: UIPresentationController) = back()
}
