package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import android.app.PendingIntent
import androidx.core.graphics.createBitmap
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.viewinterop.AndroidView
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.R
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarCells
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Narrow composition factory: immutable feature snapshots and caller-owned platform actions. */
fun historyCalendarWidgetViews(context: Context, month: HistoryMonth, today: String,
    activity: HistoryActivityMonth? = null, photos: Map<String, Bitmap> = emptyMap(),
    status: HistoryWidgetStatus = HistoryWidgetStatus.Loading,
    open: PendingIntent? = null, refresh: PendingIntent? = null): RemoteViews {
    val palette = WidgetPalette
    val views = RemoteViews(context.packageName, R.layout.history_widget)
    widgetHeading(views, month, palette)
    views.setTextViewText(R.id.history_widget_status, widgetStatus(context, activity, status))
    views.removeAllViews(R.id.history_widget_weeks)
    widgetWeekdays(context, views, palette)
    historyCalendarCells(month).chunked(7).forEach { week ->
        val row = RemoteViews(context.packageName, R.layout.history_widget_week)
        week.forEach { day -> row.addView(R.id.history_widget_week,
            widgetDay(context, day?.let(month::date), today, activity, photos, palette, open)) }
        views.addView(R.id.history_widget_weeks, row)
    }
    bindWidgetActions(context, views, open, refresh)
    return views
}

private fun widgetHeading(views: RemoteViews, month: HistoryMonth, palette: WidgetPalette) {
    val locale = Locale.forLanguageTag("ru")
    val title = LocalDate.of(month.year, month.month, 1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    views.setTextViewText(R.id.history_widget_title, title.replaceFirstChar { it.titlecase(locale) })
    palette.text(views, R.id.history_widget_title)
    palette.secondary(views, R.id.history_widget_status)
}

enum class HistoryWidgetStatus { Loading, SignedOut, Error, Ready }

private fun bindWidgetActions(context: Context, views: RemoteViews, open: PendingIntent?, refresh: PendingIntent?) {
    views.setContentDescription(R.id.history_widget_heading, context.getString(R.string.history_widget_open))
    open?.let {
        views.setOnClickPendingIntent(R.id.history_widget_heading, it)
        views.setOnClickPendingIntent(R.id.history_widget_status, it)
    }
    refresh?.let { views.setOnClickPendingIntent(R.id.history_widget_refresh, it) }
}

private fun widgetStatus(context: Context, activity: HistoryActivityMonth?, status: HistoryWidgetStatus): String =
    context.getString(when {
        status == HistoryWidgetStatus.SignedOut -> R.string.history_widget_signed_out
        status == HistoryWidgetStatus.Loading -> R.string.history_widget_loading
        status == HistoryWidgetStatus.Error -> R.string.history_widget_error
        activity?.isPartial == true -> R.string.history_widget_partial
        activity?.days.isNullOrEmpty() -> R.string.history_widget_empty
        else -> R.string.history_widget_active_days
    }, activity?.days?.size ?: 0)

private fun widgetWeekdays(context: Context, views: RemoteViews, palette: WidgetPalette) {
    views.removeAllViews(R.id.history_widget_weekdays)
    context.resources.getStringArray(R.array.history_widget_weekdays).forEach { label ->
        val child = RemoteViews(context.packageName, R.layout.history_widget_weekday)
        child.setTextViewText(R.id.history_widget_weekday, label)
        palette.secondary(child, R.id.history_widget_weekday)
        views.addView(R.id.history_widget_weekdays, child)
    }
}

private fun widgetDay(context: Context, date: String?, today: String, activity: HistoryActivityMonth?,
    photos: Map<String, Bitmap>, palette: WidgetPalette, open: PendingIntent?): RemoteViews {
    val cell = RemoteViews(context.packageName, R.layout.history_widget_day)
    if (date == null) {
        cell.setViewVisibility(R.id.history_widget_day, View.INVISIBLE)
        return cell
    }
    val count = activity?.days?.get(date)?.count ?: 0
    val photo = photos[date]
    cell.setTextViewText(R.id.history_widget_number, date.takeLast(2).toInt().toString())
    palette.text(cell, R.id.history_widget_number, photo != null)
    cell.setInt(R.id.history_widget_number, "setBackgroundColor", if (photo != null) 0x99000000.toInt() else 0)
    decorateWidgetDay(cell, photo, count, date == today, palette)
    val description = if (date == today) R.string.history_widget_today_description else R.string.history_widget_day_description
    cell.setContentDescription(R.id.history_widget_day, context.getString(description, widgetDateLabel(date), count))
    open?.let { cell.setOnClickPendingIntent(R.id.history_widget_day, it) }
    return cell
}

private fun widgetDateLabel(date: String): String = LocalDate.parse(date)
    .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")))

private fun decorateWidgetDay(cell: RemoteViews, photo: Bitmap?, count: Long, today: Boolean, palette: WidgetPalette) {
    cell.setInt(R.id.history_widget_day, "setBackgroundResource",
        if (today) R.drawable.history_widget_today else R.drawable.history_widget_day_background)
    cell.setImageViewBitmap(R.id.history_widget_photo, photo)
    cell.setViewVisibility(R.id.history_widget_photo, if (photo != null) View.VISIBLE else View.GONE)
    cell.setTextColor(R.id.history_widget_dot, palette.accent)
    cell.setViewVisibility(R.id.history_widget_dot, if (count > 0 && photo == null) View.VISIBLE else View.GONE)
    cell.setInt(R.id.history_widget_photo, "setAlpha", if (today) 210 else 255)
}

private object WidgetPalette {
    val accent = CoffeePeekColors.Primary.toArgb()

    fun text(views: RemoteViews, id: Int, onPhoto: Boolean = false) {
        if (onPhoto) views.setTextColor(id, android.graphics.Color.WHITE)
        else color(views, id, CoffeePeekColors.LightTextPrimary.toArgb(), CoffeePeekColors.DarkTextPrimary.toArgb())
    }

    fun secondary(views: RemoteViews, id: Int) =
        color(views, id, CoffeePeekColors.LightTextSecondary.toArgb(), CoffeePeekColors.DarkTextSecondary.toArgb())

    private fun color(views: RemoteViews, id: Int, light: Int, night: Int) {
        views.setColorInt(id, "setTextColor", light, night)
    }
}

@PreviewLightDark
@Composable
private fun HistoryCalendarWidgetPreview() {
    val context = LocalContext.current
    val month = HistoryMonth(2026, 10)
    val snapshot = HistoryActivityMonth(month, mapOf("2026-10-14" to HistoryActivityDay(2, null)), false)
    val photo = createBitmap(72, 72).apply {
        eraseColor(0xFF986B45.toInt())
        android.graphics.Canvas(this).drawCircle(36f, 32f, 24f, android.graphics.Paint().apply { color = 0xFFF4E2C8.toInt() })
        android.graphics.Canvas(this).drawCircle(36f, 32f, 18f, android.graphics.Paint().apply { color = 0xFF51331D.toInt() })
    }
    AndroidView(modifier = Modifier.size(340.dp, 360.dp), factory = {
        historyCalendarWidgetViews(context, month, "2026-10-14", snapshot, mapOf("2026-10-14" to photo), HistoryWidgetStatus.Ready).apply(context, null)
    })
}
