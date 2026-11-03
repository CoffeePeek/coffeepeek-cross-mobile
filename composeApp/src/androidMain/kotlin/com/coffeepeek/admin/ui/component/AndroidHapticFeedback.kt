package com.coffeepeek.admin.ui.component

import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.ref.WeakReference

/** Application presentation bridge; only the resumed host can deliver feedback. */
internal object AndroidHapticFeedback {
    @Volatile private var host = WeakReference<View>(null)

    fun bind(view: View) {
        host = WeakReference(view)
    }

    fun unbind(view: View) {
        if (host.get() === view) host = WeakReference(null)
    }

    fun perform(event: HapticEvent) {
        val owner = host
        val view = owner.get() ?: return
        view.post {
            if (host !== owner || !view.isAttachedToWindow || !view.isShown) return@post
            view.performHapticFeedback(when (event) {
                HapticEvent.Selection -> HapticFeedbackConstants.CLOCK_TICK
                HapticEvent.Success -> HapticFeedbackConstants.CONFIRM
                HapticEvent.Error -> HapticFeedbackConstants.REJECT
            })
        }
    }
}
