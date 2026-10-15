package com.coffeepeek.feature.shopreport.impl

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shopreport.impl.ui.compose.ShopReportScreenContent
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportAction
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportState

/** Self-targeted fake UI: no application DI, session, transport or submission. */
class ShopReportTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.getBooleanExtra("report_controls_preview", false)) return
        val dark = intent.getBooleanExtra("parity_dark", true)
        enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent {
            var state by remember { mutableStateOf(ShopReportState()) }
            CoffeePeekTheme(darkTheme = dark) {
                ShopReportScreenContent(state, "Happy Coffee") { action ->
                    state = when (action) {
                        is ShopReportAction.SelectCategory -> state.copy(selectedCategory = action.category)
                        is ShopReportAction.ChangeDescription -> state.copy(description = action.description)
                        else -> state // Preview never sends a report.
                    }
                }
            }
        }
    }
}
