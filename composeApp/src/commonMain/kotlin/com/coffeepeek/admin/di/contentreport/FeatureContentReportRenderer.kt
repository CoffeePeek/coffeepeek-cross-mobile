package com.coffeepeek.admin.di.contentreport

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.review.ContentReportScreenRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import com.coffeepeek.feature.contentreport.impl.createContentReportEntry

internal class FeatureContentReportRenderer(private val sessions: SessionReader, private val repository: ContentReportRepository) : ContentReportScreenRenderer {
    @Composable override fun Content(id: String, isCheckIn: Boolean, isPreview: Boolean) {
        if (isPreview) {
            val entry = remember(repository) { createContentReportEntry(repository) }
            entry.Content(id, isCheckIn, true, false, true, Navigator::popBack, {}, {})
        } else Host(id, isCheckIn)
    }

    @Composable private fun Host(id: String, isCheckIn: Boolean) {
        val host = viewModel(key = "content-report-account") { ContentReportHostViewModel(sessions) }
        val state by host.state.collectAsState()
        key(state.generation) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides state.session) {
                BoundContent(host, state, id, isCheckIn)
            }
        }
    }

    @Composable private fun BoundContent(host: ContentReportHostViewModel, state: ContentReportHostState, id: String, isCheckIn: Boolean) {
        var active by remember(state) { mutableStateOf(true) }
        DisposableEffect(state) { onDispose { active = false } }
        val owned = { active && host.state.value === state }
        val entry = remember(state) { createContentReportEntry(state.session.bind(repository), state.session::isCurrent) }
        entry.Content(id, isCheckIn, false, state.session.isLoggedIn, !state.unavailable,
            { if (owned()) Navigator.popBack() },
            { if (owned() && state.session.isCurrent()) Navigator.navigate(Navigator.Screen.Auth) },
            { if (owned()) host.retry() })
    }
}
