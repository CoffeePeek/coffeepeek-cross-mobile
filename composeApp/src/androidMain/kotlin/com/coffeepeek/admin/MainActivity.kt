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

    private val notificationPermission = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
        com.coffeepeek.admin.notifications.AndroidPush.refresh()
    }

    private val isAppReady = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppReady.get() }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        _context = this
        com.coffeepeek.admin.notifications.AndroidPush.requestPermission = {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val preferences = getSharedPreferences("push_device", Context.MODE_PRIVATE)
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    !preferences.getBoolean("asked", false)) {
                    preferences.edit().putBoolean("asked", true).apply()
                    notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        com.coffeepeek.admin.notifications.AndroidPush.requestPermissionFromSettings = {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    !getSharedPreferences("push_device", Context.MODE_PRIVATE).getBoolean("asked", false) &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    com.coffeepeek.admin.notifications.AndroidPush.requestPermission()
                } else if (!com.coffeepeek.admin.notifications.AndroidPush.permitted()) {
                    val settings = if (android.os.Build.VERSION.SDK_INT >= 26)
                        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                    else Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))
                    startActivity(settings)
                }
            }
        }
        if (savedInstanceState == null) handleAppLink(intent)
        setContent {
            App(onReady = { isAppReady.set(true) })
        }
    }

    override fun onResume() {
        super.onResume()
        com.coffeepeek.admin.notifications.AndroidPush.refresh()
    }

    override fun onDestroy() {
        com.coffeepeek.admin.notifications.AndroidPush.requestPermission = {}
        com.coffeepeek.admin.notifications.AndroidPush.requestPermissionFromSettings = {}
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
