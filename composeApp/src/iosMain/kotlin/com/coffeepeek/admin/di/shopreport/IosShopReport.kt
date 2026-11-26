package com.coffeepeek.admin.di.shopreport

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.NativeShopReportState
import com.coffeepeek.feature.shopreport.impl.createNativeShopReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Swift-compatible values without exported repositories, credentials, or transport models. */
data class IosShopReportState(
    val selectedCategory: String? = null,
    val descriptionText: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val deliveryUnconfirmed: Boolean = false,
    val error: String? = null,
    val sessionEnded: Boolean = false,
)

class IosShopReport internal constructor(
    shopId: String,
    repository: ShopIssueReportRepository,
    private val sessions: SessionReader,
) {
    private val generation = sessions.accountGeneration()
    private val report = createNativeShopReport(shopId, SessionShopReportRepository(repository, sessions, generation))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val state = MutableStateFlow(report.currentState().toIos())
    private var closed = false

    init {
        scope.launch { report.states.collect { if (isActive()) state.value = it.toIos() } }
        scope.launch { sessions.observeSession().collect { if (!isActive()) close() } }
    }

    fun currentState(): IosShopReportState = state.value

    fun observe(onState: (IosShopReportState) -> Unit): IosShopReportObservation {
        val job = CoroutineScope(Dispatchers.Main.immediate).launch { state.collect(onState) }
        return IosShopReportObservation(job)
    }

    fun selectCategory(category: String) {
        if (isActive()) report.selectCategory(category) else close()
    }

    fun changeDescription(description: String) {
        if (isActive()) report.changeDescription(description) else close()
    }

    fun submit() {
        if (isActive()) report.submit() else close()
    }

    fun close() {
        if (closed) return
        closed = true
        report.close()
        state.value = IosShopReportState(sessionEnded = true)
        scope.cancel()
    }

    private fun isActive() = !closed && sessions.accountGeneration() == generation
}

class IosShopReportObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

private fun NativeShopReportState.toIos() = IosShopReportState(
    selectedCategory, description, isSubmitting, isSubmitted, deliveryUnconfirmed, error,
)

/** Queued form actions cannot send a previous account's draft with replacement credentials. */
private class SessionShopReportRepository(
    private val repository: ShopIssueReportRepository,
    private val sessions: SessionReader,
    private val generation: Long,
) : ShopIssueReportRepository {
    override suspend fun submitReport(shopId: String, category: ShopIssueCategory, description: String?): Result<Unit> {
        checkAccount()
        val result = repository.submitReport(shopId, category, description)
        checkAccount()
        return result
    }

    private fun checkAccount() {
        if (sessions.accountGeneration() != generation) throw CancellationException("Report account changed")
    }
}
