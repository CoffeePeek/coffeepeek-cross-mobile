package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.admin.analytics.safeAnalyticsError
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.catalogBrewMethodSlug
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.CancellationException

/** Current catalogue implementation stays app-owned; late/revoked results never escape its lease. */
internal class AndroidHomeCheckInCatalog(
    private val lease: AndroidCheckInHistorySession,
    private val picker: CoffeePicker,
    private val methods: BrewMethodCatalog,
) : HomeCheckInEditorCatalog {
    override suspend fun brewMethods() = lease.withCurrentAccount {
        methods.load().map { rows -> rows.map { method ->
            HomeCheckInBrewMethod(method.slug, method.name,
                setOf("pour_over", "batch_drip", "moka_pot", "cezve").filter { it.catalogBrewMethodSlug() == method.slug }.toSet())
        } }.homeCatalogResult()
    }
    override suspend fun searchBeans(query: String) = lease.withCurrentAccount {
        picker.search(query).map { rows -> rows.map { HomeCheckInBeanOption(it.slug, it.name, it.roasterName) } }.homeCatalogResult()
    }
}

/** Hide legacy transport failures/messages at this composition boundary. */
private fun <T> Result<T>.homeCatalogResult(): Result<T> = fold({ Result.success(it) }, { error ->
    if (error is CancellationException) throw error
    if (error is HomePreparationFailure) Result.failure(error)
    else Result.failure(HomePreparationFailure(when (safeAnalyticsError(error).category) {
        "authentication" -> HomePreparationIssue.Unauthorized
        "permission" -> HomePreparationIssue.Forbidden
        "not_found" -> HomePreparationIssue.NotFound
        "rate_limited" -> HomePreparationIssue.RateLimited
        "server" -> HomePreparationIssue.Unavailable
        "network" -> HomePreparationIssue.Offline
        else -> HomePreparationIssue.RequestFailed
    }))
})

/** Guards the prepared repository without routing writes back to the legacy Home writer. */
internal class AndroidHomeCheckInRepository(
    private val lease: AndroidCheckInHistorySession,
    private val source: HomePreparationRepository,
) : HomePreparationRepository {
    override suspend fun detail(id: String) = lease.withCurrentAccount { source.detail(id) }
    override suspend fun create(input: HomePreparationInput) = lease.withCurrentAccount { source.create(input) }
    override suspend fun update(id: String, input: HomePreparationInput) = lease.withCurrentAccount { source.update(id, input) }
    override suspend fun delete(id: String) = lease.withCurrentAccount { source.delete(id) }
    override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?) =
        lease.withCurrentAccount { source.page(page, pageSize, range) }
    override suspend fun calendar(range: HomePreparationDateRange) = lease.withCurrentAccount { source.calendar(range) }
}
