package com.coffeepeek.feature.roaster.impl

import androidx.activity.ComponentActivity

/** Self-targeted fake host, never the production Application, DI or HTTP stack. */
class RoasterTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
