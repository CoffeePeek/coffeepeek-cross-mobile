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
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.CheckInHistoryScreenContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.previewHistoryCheckIn
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryMode
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryCalendarState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryListState

/** Optional fake-state visual QA host lives only in the self-targeted test APK. */
class HistoryTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previewMode = intent.getStringExtra("preview_mode") ?: return
        val dark = intent.getBooleanExtra("preview_dark", false)
        enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent {
            val sample = previewHistoryCheckIn()
            var state by remember { mutableStateOf(CheckInHistoryState("2026-10-14", true, 0,
                HistoryCalendarState(HistoryMonth(2026, 10), mapOf("2026-10-13" to listOf(sample.copy(id = "previous")),
                    "2026-10-14" to listOf(sample)), "2026-10-14", true),
                HistoryListState(listOf(sample), page = 1, hasLoaded = true),
                if (previewMode == "list") CheckInHistoryMode.List else CheckInHistoryMode.Calendar)) }
            CoffeePeekTheme(darkTheme = dark) {
                CheckInHistoryScreenContent(state, { action ->
                    when (action) {
                        is CheckInHistoryAction.SelectMode -> state = state.copy(mode = action.mode)
                        is CheckInHistoryAction.SelectDate -> state = state.copy(calendar = state.calendar.copy(selectedDate = action.date))
                        CheckInHistoryAction.Back -> finish()
                        else -> Unit
                    }
                }, { "14 октября 2026" })
            }
        }
    }
}
