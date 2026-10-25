package com.coffeepeek.admin.di.checkinhistory.widget

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistoryCalendar
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.domain.usecase.LoadHistoryActivityMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.widget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext

/** Android composition only: job ownership, revocable account leases and launcher publication. */
internal object ActivityCalendarWidgetRuntime {
    private const val JOB_ID = 0x43414C
    private lateinit var application: Application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var generation = 0L
    private val sessions: SessionReader get() = GlobalContext.get().get()
    private val scheduler: JobScheduler get() = application.getSystemService(JobScheduler::class.java)

    fun install(app: Application) {
        application = app
        app.registerActivityLifecycleCallbacks(HistoryWidgetLifecycle())
        scope.launch {
            sessions.observeSession().map { it?.userId?.takeIf { _ -> sessions.isActiveSession(it) } }
                .distinctUntilChanged().catch { error ->
                    if (error is CancellationException) throw error
                    cancel()
                    publish(placeholder(HistoryWidgetStatus.SignedOut))
                }.collect { refresh(clear = true) }
        }
    }

    fun refresh(clear: Boolean = false) {
        if (!::application.isInitialized || ActivityCalendarWidgetProvider.ids(application).isEmpty()) return
        generation++
        if (clear) publish(placeholder(if (signedIn()) HistoryWidgetStatus.Loading else HistoryWidgetStatus.SignedOut))
        val job = JobInfo.Builder(JOB_ID, ComponentName(application, ActivityCalendarWidgetJob::class.java))
            .setMinimumLatency(0).setOverrideDeadline(1_000).build()
        if (scheduler.schedule(job) == JobScheduler.RESULT_FAILURE) failed()
    }

    fun cancel() {
        generation++
        if (::application.isInitialized) scheduler.cancel(JOB_ID)
    }

    fun failed() {
        publish(placeholder(if (signedIn()) HistoryWidgetStatus.Error else HistoryWidgetStatus.SignedOut))
    }

    suspend fun update() {
        sessions.getSession()
        val version = generation
        val lease = AndroidCheckInHistorySession(sessions)
        if (!lease.isLoggedIn) {
            publish(placeholder(HistoryWidgetStatus.SignedOut))
            return
        }
        val calendar = AndroidCheckInHistoryCalendar().snapshot()
        val loaded = withContext(Dispatchers.IO) {
            val koin = GlobalContext.get()
            val load = LoadHistoryActivityMonth(lease.bind(koin.get<CheckInHistoryRepository>()), lease.bind(koin.get<HomeHistoryReader>()))
            val activity = loadHistoryWidgetActivity(load, calendar)
            val photos = HistoryWidgetPhotos(koin.get(), sessions).load(activity)
            activity to photos
        }
        if (version == generation && lease.isCurrent()) {
            publish(views(calendar.today, loaded.first, loaded.second, HistoryWidgetStatus.Ready))
        }
    }

    private fun signedIn() = sessions.peekSession().let { sessions.isActiveSession(it) && !it?.userId.isNullOrBlank() }

    private fun placeholder(status: HistoryWidgetStatus): RemoteViews {
        val calendar = AndroidCheckInHistoryCalendar().snapshot()
        return views(calendar.today, emptyList(), emptyMap(), status)
    }

    private fun views(today: String, activity: List<HistoryActivityMonth>, photos: Map<String, Bitmap>,
        status: HistoryWidgetStatus) = historyCalendarWidgetViews(application, today, activity, photos, status,
        historyWidgetOpenIntent(application), historyWidgetRefreshIntent(application))

    private fun publish(views: RemoteViews) {
        val ids = ActivityCalendarWidgetProvider.ids(application)
        if (ids.isNotEmpty()) AppWidgetManager.getInstance(application).updateAppWidget(ids, views)
    }
}
