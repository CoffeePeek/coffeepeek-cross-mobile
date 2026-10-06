package com.coffeepeek.admin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.concurrent.atomic.AtomicBoolean
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.appLinkScreen

class MainActivity : ComponentActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var _context: Context? = null
        val context get() = _context!!
    }

    private val isAppReady = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppReady.get() }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        _context = this
        if (savedInstanceState == null) handleAppLink(intent)
        setContent {
            App(onReady = { isAppReady.set(true) })
        }
    }

    override fun onDestroy() {
        _context = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAppLink(intent)
    }

    private fun handleAppLink(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        appLinkScreen(intent.dataString ?: return)?.let(Navigator::openAppLink)
    }
}
