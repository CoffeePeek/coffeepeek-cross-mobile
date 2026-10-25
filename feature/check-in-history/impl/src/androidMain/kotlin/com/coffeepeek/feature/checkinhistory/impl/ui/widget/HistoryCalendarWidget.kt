package com.coffeepeek.feature.checkinhistory.impl.ui.widget

import android.app.PendingIntent
import androidx.core.graphics.createBitmap
import android.content.Context
import android.graphics.Bitmap
import android.util.SizeF
import android.util.TypedValue
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
    widgetHeading(context, views, today, period, calendar)
    views.setTextViewText(R.id.history_widget_status, widgetStatus(context, calendar, status, period))
    views.setViewVisibility(R.id.history_widget_status,
        if (period == HistoryWidgetPeriod.Month || status != HistoryWidgetStatus.Ready || calendar.isPartial) View.VISIBLE else View.GONE)
    views.removeAllViews(R.id.history_widget_weeks)
    widgetWeekdays(context, views, WidgetPalette)
    calendar.dates.chunked(7).forEach { week ->
        val row = RemoteViews(context.packageName, R.layout.history_widget_week)
        week.forEach { date -> row.addView(R.id.history_widget_week,
            widgetDay(context, date, today, calendar.days, photos, WidgetPalette, open)) }
        views.addView(R.id.history_widget_weeks, row)
    }
    bindWidgetActions(context, views, open, refresh)
    return views
}

private fun widgetHeading(context: Context, views: RemoteViews, today: String,
    period: HistoryWidgetPeriod, calendar: HistoryWidgetCalendar) {
    val month = period == HistoryWidgetPeriod.Month
    views.setTextViewText(R.id.history_widget_title, widgetTitle(context, today, period, calendar.dates))
    views.setTextViewTextSize(R.id.history_widget_title, TypedValue.COMPLEX_UNIT_SP, if (month) 18f else 14f)
    views.setViewVisibility(R.id.history_widget_brand, if (month) View.VISIBLE else View.GONE)
    views.setViewVisibility(R.id.history_widget_refresh, if (month) View.VISIBLE else View.GONE)
    WidgetPalette.text(views, R.id.history_widget_title)
    WidgetPalette.secondary(views, R.id.history_widget_status)
}

private fun widgetTitle(context: Context, today: String, period: HistoryWidgetPeriod, dates: List<String?>): String {
    val locale = Locale.forLanguageTag("ru")
    if (period == HistoryWidgetPeriod.Month) return LocalDate.parse(today)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
    val start = LocalDate.parse(dates.filterNotNull().first())
    val end = LocalDate.parse(dates.filterNotNull().last())
    val format = DateTimeFormatter.ofPattern("d MMM", locale)
    val first = if (start.month == end.month) start.dayOfMonth.toString() else start.format(format)
    val title = if (period == HistoryWidgetPeriod.Week) R.string.history_widget_week_title else R.string.history_widget_fortnight_title
    return context.getString(title, "$first–${end.format(format)}")
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
        calendar.days.isEmpty() -> R.string.history_widget_empty
        else -> R.string.history_widget_active_days
    }, calendar.days.size)

private fun widgetWeekdays(context: Context, views: RemoteViews, palette: WidgetPalette) {
    views.removeAllViews(R.id.history_widget_weekdays)
    context.resources.getStringArray(R.array.history_widget_weekdays).forEach { label ->
        val child = RemoteViews(context.packageName, R.layout.history_widget_weekday)
        child.setTextViewText(R.id.history_widget_weekday, label)
        palette.secondary(child, R.id.history_widget_weekday)
        views.addView(R.id.history_widget_weekdays, child)
    }
}

private fun widgetDay(context: Context, date: String?, today: String, activity: Map<String, HistoryActivityDay>,
    photos: Map<String, Bitmap>, palette: WidgetPalette, open: PendingIntent?): RemoteViews {
    val cell = RemoteViews(context.packageName, R.layout.history_widget_day)
    if (date == null) {
        cell.setViewVisibility(R.id.history_widget_day, View.INVISIBLE)
        return cell
    }
    val count = activity[date]?.count ?: 0
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
