package com.coffeepeek.feature.checkinhistory.impl

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.CheckInHistoryScreenContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.previewCombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryAction

/** Fake visual host lives only in the self-targeted test APK. */
class HistoryTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getStringExtra("preview_mode") == null) return
        val dark = intent.getBooleanExtra("preview_dark", false)
        enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent {
            var state by remember { mutableStateOf(previewCombinedHistoryState()) }
            CoffeePeekTheme(darkTheme = dark) {
                CheckInHistoryScreenContent(state, { action ->
                    when (action) {
                        is CombinedHistoryAction.SelectFilter -> state = state.copy(filter = action.filter)
                        is CombinedHistoryAction.SelectDate -> state = state.copy(selectedDate = action.date.takeUnless { it == state.selectedDate })
                        CombinedHistoryAction.Today -> state = state.copy(selectedDate = state.today)
                        CombinedHistoryAction.ShowCreate -> state = state.copy(showCreate = true)
                        CombinedHistoryAction.DismissCreate -> state = state.copy(showCreate = false)
                        is CombinedHistoryAction.Create -> state = state.copy(showCreate = false)
                        is CombinedHistoryAction.OpenEntry -> state = state.copy(selectedShopKey = action.key)
                        CombinedHistoryAction.DismissShop -> state = state.copy(selectedShopKey = null)
                        CombinedHistoryAction.Back -> finish()
                        else -> Unit
                    }
                }, { "14 октября 2026" }, { "13:00" })
            }
        }
    }
}
