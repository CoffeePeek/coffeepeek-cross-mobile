package com.coffeepeek.admin.di.checkinhistory.widget

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.*

/** A scheduled job owns the process lifetime while the read and bounded photo downloads run. */
class ActivityCalendarWidgetJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running?.cancel()
        running = scope.launch { update(params) }
        return true
    }

    private suspend fun update(params: JobParameters) {
        try {
            withTimeout(60_000) { ActivityCalendarWidgetRuntime.update() }
            jobFinished(params, false)
        } catch (_: TimeoutCancellationException) {
            ActivityCalendarWidgetRuntime.failed()
            jobFinished(params, false)
        } catch (error: CancellationException) {
            if (currentCoroutineContext().isActive) jobFinished(params, false)
            throw error
        } catch (_: Exception) {
            ActivityCalendarWidgetRuntime.failed()
            jobFinished(params, false)
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
