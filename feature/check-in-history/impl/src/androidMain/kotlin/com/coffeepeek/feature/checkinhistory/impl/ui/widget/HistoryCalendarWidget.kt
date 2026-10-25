package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import android.app.PendingIntent
import androidx.core.graphics.createBitmap
import android.content.Context
import android.graphics.Bitmap
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.viewinterop.AndroidView
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Android selects a complete week, fortnight or month immediately as the host resizes. */
fun historyCalendarWidgetViews(context: Context, today: String,
    activity: List<HistoryActivityMonth> = emptyList(), photos: Map<String, Bitmap> = emptyMap(),
    status: HistoryWidgetStatus = HistoryWidgetStatus.Loading,
    open: PendingIntent? = null, refresh: PendingIntent? = null): RemoteViews = RemoteViews(
    HistoryWidgetPeriod.entries.associate { period ->
        SizeF(180f, period.minimumHeightDp) to widgetLayout(context, today, period, activity, photos, status, open, refresh)
    },
)

private fun widgetLayout(context: Context, today: String, period: HistoryWidgetPeriod,
    activity: List<HistoryActivityMonth>, photos: Map<String, Bitmap>, status: HistoryWidgetStatus,
    open: PendingIntent? = null, refresh: PendingIntent? = null): RemoteViews {
    val calendar = historyWidgetCalendar(today, period, activity)
    val views = RemoteViews(context.packageName, R.layout.history_widget)
    widgetHeading(views, today, period)
    views.setTextViewText(R.id.history_widget_status, widgetStatus(context, calendar, status, period))
    widgetStatusDescription(context, views, calendar, status, period)
    views.removeAllViews(R.id.history_widget_weeks)
    widgetWeekdays(context, views, today, period)
    calendar.dates.chunked(7).forEach { week ->
        val row = RemoteViews(context.packageName, R.layout.history_widget_week)
        week.forEach { date -> row.addView(R.id.history_widget_week,
            historyWidgetDay(context, date, today, calendar.days, photos, open)) }
        views.addView(R.id.history_widget_weeks, row)
    }
    bindWidgetActions(context, views, open, refresh)
    return views
}

private fun widgetHeading(views: RemoteViews, today: String, period: HistoryWidgetPeriod) {
    views.setViewVisibility(R.id.history_widget_header, if (period == HistoryWidgetPeriod.Month) View.VISIBLE else View.GONE)
    val locale = Locale.forLanguageTag("ru")
    val title = LocalDate.parse(today).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
    views.setTextViewText(R.id.history_widget_title, title)
    HistoryWidgetPalette.text(views, R.id.history_widget_title)
    HistoryWidgetPalette.secondary(views, R.id.history_widget_status)
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

private fun widgetStatus(context: Context, calendar: HistoryWidgetCalendar, status: HistoryWidgetStatus,
    period: HistoryWidgetPeriod): String =
    context.getString(when {
        status == HistoryWidgetStatus.SignedOut -> R.string.history_widget_signed_out
        status == HistoryWidgetStatus.Loading -> R.string.history_widget_loading
        status == HistoryWidgetStatus.Error -> if (period == HistoryWidgetPeriod.Month)
            R.string.history_widget_error else R.string.history_widget_error_compact
        calendar.isPartial -> if (period == HistoryWidgetPeriod.Month)
            R.string.history_widget_partial else R.string.history_widget_partial_compact
        else -> R.string.history_widget_month_progress
    }, calendar.activeMonthDays, calendar.daysInMonth)

private fun widgetStatusDescription(context: Context, views: RemoteViews, calendar: HistoryWidgetCalendar,
    status: HistoryWidgetStatus, period: HistoryWidgetPeriod) {
    val description = if (status == HistoryWidgetStatus.Ready && !calendar.isPartial)
        context.getString(R.string.history_widget_month_progress_description, calendar.activeMonthDays, calendar.daysInMonth)
    else widgetStatus(context, calendar, status, period)
    views.setContentDescription(R.id.history_widget_status, description)
}

private fun widgetWeekdays(context: Context, views: RemoteViews, today: String, period: HistoryWidgetPeriod) {
    views.removeAllViews(R.id.history_widget_weekdays)
    val weekday = LocalDate.parse(today).dayOfWeek.value - 1
    context.resources.getStringArray(R.array.history_widget_weekdays).forEachIndexed { index, label ->
        val child = RemoteViews(context.packageName, R.layout.history_widget_weekday)
        child.setTextViewText(R.id.history_widget_weekday, label)
        if (period != HistoryWidgetPeriod.Month && index == weekday) child.setTextColor(R.id.history_widget_weekday, HistoryWidgetPalette.accent)
        else HistoryWidgetPalette.secondary(child, R.id.history_widget_weekday)
        views.addView(R.id.history_widget_weekdays, child)
    }
}

@PreviewLightDark
@Composable
private fun HistoryCalendarWeekPreview() = HistoryCalendarPreview(HistoryWidgetPeriod.Week, 80)

@PreviewLightDark
@Composable
private fun HistoryCalendarFortnightPreview() = HistoryCalendarPreview(HistoryWidgetPeriod.Fortnight, 160)

@PreviewLightDark
@Composable
private fun HistoryCalendarMonthPreview() = HistoryCalendarPreview(HistoryWidgetPeriod.Month, 300)

@Composable
private fun HistoryCalendarPreview(period: HistoryWidgetPeriod, height: Int) {
    val context = LocalContext.current
    val today = "2026-03-02"
    val dates = listOf("2026-02-24", "2026-02-27", today)
    val snapshots = dates.groupBy { requireNotNull(HistoryMonth.fromDate(it)) }.map { (month, days) ->
        HistoryActivityMonth(month, days.associateWith { HistoryActivityDay(2, null) }, false)
    }
    val photo = createBitmap(72, 72).apply {
        eraseColor(0xFF986B45.toInt())
        android.graphics.Canvas(this).drawCircle(36f, 32f, 24f, android.graphics.Paint().apply { color = 0xFFF4E2C8.toInt() })
        android.graphics.Canvas(this).drawCircle(36f, 32f, 18f, android.graphics.Paint().apply { color = 0xFF51331D.toInt() })
    }
    AndroidView(modifier = Modifier.size(300.dp, height.dp), factory = {
        widgetLayout(context, today, period, snapshots, dates.associateWith { photo }, HistoryWidgetStatus.Ready).apply(context, null)
    })
}
