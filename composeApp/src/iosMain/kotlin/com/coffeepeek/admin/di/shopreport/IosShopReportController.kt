package com.coffeepeek.admin.di.shopreport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shopreport.data.repository.createShopIssueReportRepository
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosShopReportController =
    staticCompositionLocalOf<(IosShopReport, String, String, () -> Unit) -> UIViewController> {
        error("The iOS app must provide a shop report controller")
    }

@Composable
internal fun iosShopReportContent(): (String, String) -> (() -> UIViewController) {
    val clients = koinInject<ApiClients>()
    val sessions = koinInject<SessionReader>()
    val factory = LocalIosShopReportController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    return iosShopReportContent(factory, { languageTag }, clients, sessions)
}

internal fun iosShopReportContent(factory: (IosShopReport, String, String, () -> Unit) -> UIViewController,
    language: () -> String, clients: ApiClients, sessions: SessionReader,
): (String, String) -> (() -> UIViewController) {
    return { shopId, shopTitle -> {
        val report = IosShopReport(
            shopId, createShopIssueReportRepository(clients.client), sessions,
        )
        factory(report, language(), shopTitle, Navigator::popBack)
    } }
}
