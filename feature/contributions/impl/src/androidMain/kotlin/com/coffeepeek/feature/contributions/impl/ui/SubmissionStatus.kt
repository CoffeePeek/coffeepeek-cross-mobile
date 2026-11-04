package com.coffeepeek.feature.contributions.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.contributions.impl.R

internal enum class SubmissionStatus { Sending, Error }

/** The full-screen state of a form that is sending or has failed; the form owns the request, the retry and the way back. */
@Composable
internal fun SubmissionStatusScreen(
    screenTitle: String,
    status: SubmissionStatus,
    title: String,
    description: String,
    onBack: () -> Unit,
    onFeedback: (success: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(status) {
        focus.clearFocus(force = true)
        keyboard?.hide()
        if (status == SubmissionStatus.Error) onFeedback(false)
    }
    BackHandler(enabled = status == SubmissionStatus.Error, onBack = onBack)
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(screenTitle, stringResource(R.string.contributions_back), onBack = onBack.takeIf { status == SubmissionStatus.Error }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(CoffeePeekDimens.spacing6).semantics { liveRegion = LiveRegionMode.Polite }) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = CoffeePeekDimens.spacing4),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                if (status == SubmissionStatus.Sending) CoffeePeekLoader(stringResource(R.string.contributions_loading), size = 88.dp)
                else Icon(CoffeePeekIcons.Error, contentDescription = null, modifier = Modifier.size(88.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            if (status == SubmissionStatus.Error) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) { actions() }
        }
    }
}

@Composable
internal fun SubmissionErrorActions(retryText: String, backText: String, onRetry: () -> Unit, onEdit: () -> Unit) {
    CoffeePeekButton(retryText, onRetry)
    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight)) { Text(backText) }
}
