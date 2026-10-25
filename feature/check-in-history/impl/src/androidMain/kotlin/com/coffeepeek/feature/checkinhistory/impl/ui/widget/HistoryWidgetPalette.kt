package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.coffeepeek.core.designsystem.component.CoffeePeekCalendarDayTone
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors

internal object HistoryWidgetPalette {
    val accent = CoffeePeekColors.Primary.toArgb()

    fun text(views: RemoteViews, id: Int, tone: CoffeePeekCalendarDayTone = CoffeePeekCalendarDayTone.Content) {
        when (tone) {
            CoffeePeekCalendarDayTone.Accent -> views.setTextColor(id, accent)
            CoffeePeekCalendarDayTone.OnAccent -> color(views, id, CoffeePeekColors.LightTextOnPrimary.toArgb(), CoffeePeekColors.DarkTextOnPrimary.toArgb())
            CoffeePeekCalendarDayTone.Muted -> color(views, id, CoffeePeekColors.LightTextPrimary.copy(alpha = 0.35f).toArgb(),
                CoffeePeekColors.DarkTextPrimary.copy(alpha = 0.35f).toArgb())
            CoffeePeekCalendarDayTone.Content -> color(views, id, CoffeePeekColors.LightTextPrimary.toArgb(), CoffeePeekColors.DarkTextPrimary.toArgb())
        }
    }

    fun secondary(views: RemoteViews, id: Int) =
        color(views, id, CoffeePeekColors.LightTextSecondary.toArgb(), CoffeePeekColors.DarkTextSecondary.toArgb())

    private fun color(views: RemoteViews, id: Int, light: Int, night: Int) {
        views.setColorInt(id, "setTextColor", light, night)
    }
}
