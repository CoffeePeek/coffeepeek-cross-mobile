package com.coffeepeek.feature.contentreport.impl

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.contentreport.api.ContentReportEntry
import com.coffeepeek.feature.contentreport.domain.model.*
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import com.coffeepeek.feature.contentreport.impl.ui.ContentReportViewModel
import com.coffeepeek.feature.contentreport.impl.ui.compose.ContentReportScreenContent
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.*

fun createContentReportEntry(repository: ContentReportRepository, isCurrent: () -> Boolean = { true }): ContentReportEntry =
    DefaultContentReportEntry(repository, isCurrent)

private class DefaultContentReportEntry(private val repository: ContentReportRepository, private val isCurrent: () -> Boolean) : ContentReportEntry {
    @Composable override fun Content(id: String, isCheckIn: Boolean, isPreview: Boolean, isLoggedIn: Boolean, accessAvailable: Boolean,
        onBack: () -> Unit, onSignIn: () -> Unit, onRetryAccess: () -> Unit) {
        val kind = if (isCheckIn) ContentReportKind.CheckIn else ContentReportKind.Review
        val model = viewModel(key = "content-report:$kind:$id:$isPreview") {
            ContentReportViewModel(ContentReportTarget(kind, id), isPreview, isLoggedIn, accessAvailable, repository, isCurrent)
        }
        val state by model.state.collectAsStateWithLifecycle()
        val owner = remember(model) { Any() }
        val back by rememberUpdatedState(onBack)
        val signIn by rememberUpdatedState(onSignIn)
        val retry by rememberUpdatedState(onRetryAccess)
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(ContentReportAction.Resume) }
        LaunchedEffect(model) {
            model.events.collect { envelope -> when (envelope.forOwner(owner)) {
                ContentReportEvent.Back -> back()
                ContentReportEvent.SignIn -> signIn()
                ContentReportEvent.RetryAccess -> retry()
                null -> Unit
            } }
        }
        ContentReportScreenContent(state) { action -> model.onAction(when (action) {
            is ContentReportAction.Back -> action.copy(owner = owner)
            is ContentReportAction.Submit -> action.copy(owner = owner)
            is ContentReportAction.RetryAccess -> action.copy(owner = owner)
            else -> action
        }) }
    }
}
