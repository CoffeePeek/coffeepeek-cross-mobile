package com.coffeepeek.admin.ui.component

/** A resource picker must open only after its source sheet has finished dismissing. */
internal class PlatformActionSelection(
    private val dismissBeforeAction: Boolean,
    private val onDismiss: () -> Unit,
) {
    private var selected = false

    fun select(action: PlatformAction, dismissPresentation: (() -> Unit) -> Unit) {
        if (selected || !action.enabled) return
        selected = true
        dismissPresentation {
            if (dismissBeforeAction) onDismiss()
            action.onClick()
        }
    }
}
