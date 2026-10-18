package com.coffeepeek.admin.sandbox

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.sandbox.compose.SandboxScreen

/** Isolated bootstrap reuses the real graph with local ports, never production services. */
class SandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: SandboxViewModel = viewModel()
            SandboxScreen(model, application as SandboxApplication)
        }
    }
    override fun startActivity(intent: Intent, options: Bundle?) {
        (application as SandboxApplication).blockedExternalAction()
    }
}
