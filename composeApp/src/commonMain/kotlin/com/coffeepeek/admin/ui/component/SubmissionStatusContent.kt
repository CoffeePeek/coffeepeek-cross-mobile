@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

internal enum class SubmissionStatus { Sending, Success, Error }

@Composable
internal fun SubmissionErrorActions(onRetry: () -> Unit, onEdit: () -> Unit) {
    AppButton("Повторить отправку", onClick = onRetry)
    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.buttonHeight)) {
        Text("Вернуться к форме")
    }
}

@Composable
internal fun SubmissionStatusScreen(
    screenTitle: String,
    status: SubmissionStatus,
    title: String,
    description: String,
    onBack: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    BackHandler(enabled = status == SubmissionStatus.Error, onBack = onBack)
    Scaffold(
        topBar = { CpTopBar(screenTitle, onBack = onBack, customBack = status == SubmissionStatus.Error) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        SubmissionStatusContent(
            status, title, description,
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(CpDimens.spacing6),
            actions = actions,
        )
    }
}

/** Presentation only: each form owns the request, retry and result actions. */
@Composable
internal fun SubmissionStatusContent(
    status: SubmissionStatus,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    fillAvailableHeight: Boolean = true,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(status) {
        focus.clearFocus(force = true)
        keyboard?.hide()
        when (status) {
            SubmissionStatus.Sending -> Unit
            SubmissionStatus.Success -> platformHaptic(HapticEvent.Success)
            SubmissionStatus.Error -> platformHaptic(HapticEvent.Error)
        }
    }
    val reduceMotion = platformReduceMotion()
    AnimatedContent(
        targetState = Triple(status, title, description),
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(if (reduceMotion) 0 else 250)) +
                scaleIn(tween(if (reduceMotion) 0 else 300), initialScale = 0.9f)) togetherWith
                fadeOut(tween(if (reduceMotion) 0 else 150))
        },
        label = "submission-status",
    ) { (current, heading, message) ->
        Column(
            (if (fillAvailableHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            PlatformScrollableColumn(
                modifier = Modifier.weight(1f, fill = fillAvailableHeight).fillMaxWidth()
                    .padding(bottom = CpDimens.spacing4).clipToBounds(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (current == SubmissionStatus.Sending) {
                    CoffeePeekLoader(size = 88.dp)
                } else {
                    Icon(
                        if (current == SubmissionStatus.Success) CpIcons.CheckCircle else CpIcons.Error,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                        tint = if (current == SubmissionStatus.Success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(CpDimens.spacing4))
                Text(heading, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(CpDimens.spacing4))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(CpDimens.spacing4))
            }
            if (current != SubmissionStatus.Sending) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                    content = actions,
                )
            }
        }
    }
}
