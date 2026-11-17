package com.coffeepeek.admin.di.contributions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.NativeChangeRequestSession
import com.coffeepeek.feature.contributions.impl.NativeContributionsListSession
import kotlinx.coroutines.Job
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosContributionsController = staticCompositionLocalOf<(IosContributionsRoute, String) -> UIViewController> {
    error("The iOS app must provide a contributions controller")
}

@Composable
internal fun iosContributionsContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val factory = LocalIosContributionsController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val repository = koinInject<ContributionsRepository>()
    val sessions = koinInject<SessionReader>()
    return { screen ->
        when (screen) {
            is Navigator.Screen.MyContributions -> { { factory(listRoute(screen.kind, repository, sessions), languageTag) } }
            is Navigator.Screen.ShopChangeRequestDetail -> { { factory(detailRoute(screen.requestId, repository, sessions), languageTag) } }
            else -> null
        }
    }
}

private fun listRoute(kind: String, repository: ContributionsRepository, sessions: SessionReader): IosContributionsList {
    val contributionKind = ContributionKind.valueOf(kind)
    val session = NativeContributionsListSession(contributionKind, repository, SessionContributionsAccount(sessions))
    return IosContributionsList(contributionKind.name.lowercase(), session)
}

private fun detailRoute(id: String, repository: ContributionsRepository, sessions: SessionReader) =
    IosChangeRequest(NativeChangeRequestSession(id, repository, SessionContributionsAccount(sessions)))

sealed class IosContributionsRoute {
    fun back() = Navigator.popBack()
    fun signIn() = Navigator.navigate(Navigator.Screen.Auth)
    abstract fun close()
}

class IosContributionsList internal constructor(val kind: String, private val session: NativeContributionsListSession) :
    IosContributionsRoute() {
    fun observe(receive: (IosContributionsListState) -> Unit) =
        IosContributionObservation(session.observe { receive(it.ios()) })

    fun refresh() = session.refresh()
    fun select(status: String) = session.select(status)
    fun loadMore(status: String) = session.loadMore(status)

    fun open(id: String) {
        val (shop, target) = session.destination(id) ?: return
        Navigator.navigate(if (shop) Navigator.Screen.ShopDetail(target) else Navigator.Screen.ShopChangeRequestDetail(target))
    }

    override fun close() = session.close()
}

class IosChangeRequest internal constructor(private val session: NativeChangeRequestSession) : IosContributionsRoute() {
    fun observe(receive: (IosChangeRequestState) -> Unit) =
        IosContributionObservation(session.observe { receive(it.ios()) })

    fun refresh() = session.refresh()
    fun openShop() { session.shop()?.let { Navigator.navigate(Navigator.Screen.ShopDetail(it)) } }
    override fun close() = session.close()
}

class IosContributionObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
