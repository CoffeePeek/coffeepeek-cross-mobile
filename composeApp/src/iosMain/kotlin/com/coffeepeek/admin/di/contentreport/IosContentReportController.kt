package com.coffeepeek.admin.di.contentreport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosContentReportController =
    staticCompositionLocalOf<(IosContentReport, String) -> UIViewController> {
        error("The iOS app must provide a content report controller")
    }

@Composable
internal fun iosContentReportContent(): (String, Boolean, Boolean) -> (() -> UIViewController) {
    val factory = LocalIosContentReportController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val sessions = koinInject<SessionReader>()
    val repository = koinInject<ContentReportRepository>()
    return iosContentReportContent(factory, { languageTag }, sessions, repository)
}

internal fun iosContentReportContent(factory: (IosContentReport, String) -> UIViewController, language: () -> String,
    sessions: SessionReader, repository: ContentReportRepository,
): (String, Boolean, Boolean) -> (() -> UIViewController) {
    return { id, isCheckIn, preview -> {
        val report = IosContentReport(id, isCheckIn, preview, sessions, repository, Navigator::popBack) {
            Navigator.navigate(Navigator.Screen.Auth)
        }
        factory(report, language())
    } }
}
