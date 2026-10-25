package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.coffeepeek.core.designsystem.component.coffeePeekCalendarDayStyle
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryActivityDay
import com.coffeepeek.feature.checkinhistory.impl.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun historyWidgetDay(context: Context, date: String?, today: String, activity: Map<String, HistoryActivityDay>,
    photos: Map<String, Bitmap>, open: PendingIntent?): RemoteViews {
    val cell = RemoteViews(context.packageName, R.layout.history_widget_day)
    if (date == null) {
        cell.setViewVisibility(R.id.history_widget_day, View.INVISIBLE)
        return cell
    }
    val photo = photos[date]
    val style = coffeePeekCalendarDayStyle(date == today, date > today, photo != null)
    cell.setTextViewText(R.id.history_widget_number, date.takeLast(2).toInt().toString())
    cell.setViewVisibility(R.id.history_widget_number, if (style.showDate) View.VISIBLE else View.GONE)
    HistoryWidgetPalette.text(cell, R.id.history_widget_number, style.tone)
    cell.setInt(R.id.history_widget_day, "setBackgroundResource",
        if (style.outlined) R.drawable.history_widget_today else R.drawable.history_widget_day_background)
    cell.setImageViewBitmap(R.id.history_widget_photo, photo)
    cell.setViewVisibility(R.id.history_widget_photo, if (photo != null) View.VISIBLE else View.GONE)
    describeWidgetDay(context, cell, date, today, activity[date]?.count ?: 0)
    open?.let { cell.setOnClickPendingIntent(R.id.history_widget_day, it) }
    return cell
}

private fun describeWidgetDay(context: Context, cell: RemoteViews, date: String, today: String, count: Long) {
    val description = if (date == today) R.string.history_widget_today_description else R.string.history_widget_day_description
    val label = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")))
    cell.setContentDescription(R.id.history_widget_day, context.getString(description, label, count))
}

@PreviewLightDark
@Composable
private fun HistoryWidgetDayPreview() {
    val context = LocalContext.current
    AndroidView(modifier = Modifier.size(48.dp), factory = {
        historyWidgetDay(context, "2026-10-25", "2026-10-25", emptyMap(), emptyMap(), null).apply(context, null)
    })
}
