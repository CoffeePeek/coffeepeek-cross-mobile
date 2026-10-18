package com.coffeepeek.feature.homecheckin.impl

import androidx.activity.ComponentActivity

/** Self-targeted host only: no production Application, Koin, account or network. */
class HomeCheckInTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Scoped to this fake test window, not a device setting or keyguard override.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
