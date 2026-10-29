package com.coffeepeek.feature.contributions.impl.ui

import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_back
import com.coffeepeek.feature.contributions.impl.resources.contributions_loading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.coffeepeek.core.designsystem.component.coffeePeekViewportBottomPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
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
    CoffeePeekBackHandler(enabled = status == SubmissionStatus.Error, onBack = onBack)
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(screenTitle, stringResource(Res.string.contributions_back), onBack = onBack.takeIf { status == SubmissionStatus.Error }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).coffeePeekViewportBottomPadding().coffeePeekScreenBottomPadding().padding(CoffeePeekDimens.spacing6).semantics { liveRegion = LiveRegionMode.Polite }) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = CoffeePeekDimens.spacing4),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                if (status == SubmissionStatus.Sending) CoffeePeekLoader(stringResource(Res.string.contributions_loading), size = 88.dp)
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

@Composable
private fun SubmissionStatusPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SubmissionStatusScreen(
        stringResource(Res.string.contributions_loading), SubmissionStatus.Sending,
        stringResource(Res.string.contributions_loading), "", {}, {},
    )
}

@Preview @Composable private fun SubmissionStatusLightPreview() = SubmissionStatusPreview(false)
@Preview @Composable private fun SubmissionStatusDarkPreview() = SubmissionStatusPreview(true)
