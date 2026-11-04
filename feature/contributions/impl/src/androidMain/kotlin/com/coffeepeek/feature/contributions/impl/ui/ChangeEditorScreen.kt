package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.ChangeEditorHost
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.R
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorAction
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorState
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorViewModel
import com.coffeepeek.feature.contributions.impl.change.EditorLoadError
import com.coffeepeek.feature.contributions.impl.change.EditorSubmit

internal class EditorCallbacks(val onBack: () -> Unit, val onSignIn: () -> Unit, val onOpenRequest: (String) -> Unit)

@Composable
internal fun ChangeEditorScreen(viewModel: ChangeEditorViewModel, editing: Boolean, host: ChangeEditorHost, callbacks: EditorCallbacks, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChangeEditorContent(state, editing, host, callbacks, viewModel::onAction, modifier)
}

@Composable
internal fun ChangeEditorContent(
    state: ChangeEditorState, editing: Boolean, host: ChangeEditorHost, callbacks: EditorCallbacks, onAction: (ChangeEditorAction) -> Unit, modifier: Modifier = Modifier,
) {
    val title = state.section.title()
    val submit = state.submit
    when {
        state.signedIn == false -> FormSignInPrompt(title, stringResource(R.string.contributions_editor_sign_in_prompt), callbacks.onBack, callbacks.onSignIn, modifier)
        submit is EditorSubmit.Done -> EditorSuccess(title, submit.request, editing, host, callbacks, modifier)
        submit == EditorSubmit.Sending -> SubmissionStatusScreen(
            title, SubmissionStatus.Sending, stringResource(if (editing) R.string.contributions_editor_saving_title else R.string.contributions_editor_sending_title),
            stringResource(R.string.contributions_editor_sending_description), callbacks.onBack, host.feedback, modifier,
        )
        submit is EditorSubmit.Failed -> SubmissionStatusScreen(
            title, SubmissionStatus.Error, stringResource(R.string.contributions_editor_failed_title),
            "${submit.serverMessage ?: stringResource(R.string.contributions_editor_failed_default)}\n\n${stringResource(R.string.contributions_shop_failed_kept)}",
            { onAction(ChangeEditorAction.DismissFailure) }, host.feedback, modifier,
        ) {
            SubmissionErrorActions(
                stringResource(R.string.contributions_shop_retry_send), stringResource(R.string.contributions_shop_back_to_form),
                { onAction(ChangeEditorAction.Submit) }, { onAction(ChangeEditorAction.DismissFailure) },
            )
        }
        else -> Scaffold(
            modifier = modifier,
            topBar = { CoffeePeekTopBar(title, stringResource(R.string.contributions_back), onBack = callbacks.onBack) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            when {
                state.signedIn == null || state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CoffeePeekLoader(stringResource(R.string.contributions_loading)) }
                state.loadError != null -> LoadFailure(state, onAction, callbacks, Modifier.padding(padding))
                else -> Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().padding(horizontal = CoffeePeekDimens.spacing4)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        if (state.shopTitle.isNotBlank()) {
                            Text(state.shopTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(CoffeePeekDimens.spacing3))
                        }
                        ChangeEditorBody(state, host, onAction)
                        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                    }
                    CoffeePeekButton(
                        stringResource(if (editing) R.string.contributions_editor_save else R.string.contributions_shop_submit),
                        { onAction(ChangeEditorAction.Submit) }, enabled = state.canSubmit,
                    )
                    Spacer(Modifier.height(CoffeePeekDimens.spacing3))
                }
            }
        }
    }
}

@Composable
private fun LoadFailure(state: ChangeEditorState, onAction: (ChangeEditorAction) -> Unit, callbacks: EditorCallbacks, modifier: Modifier) {
    val signIn = state.loadError == EditorLoadError.SignInRequired
    Column(modifier.fillMaxSize().padding(CoffeePeekDimens.spacing4), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            when (state.loadError) {
                EditorLoadError.SignInRequired -> stringResource(R.string.contributions_editor_sign_in_prompt)
                EditorLoadError.NotFound -> state.loadMessage ?: stringResource(R.string.contributions_editor_not_found)
                else -> state.loadMessage ?: stringResource(R.string.contributions_load_failed)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        if (state.loadError != EditorLoadError.NotFound) CoffeePeekButton(
            stringResource(if (signIn) R.string.contributions_sign_in else R.string.contributions_retry),
            { if (signIn) callbacks.onSignIn() else onAction(ChangeEditorAction.Reload) },
        )
    }
}

@Composable
private fun EditorSuccess(title: String, request: ChangeRequest, editing: Boolean, host: ChangeEditorHost, callbacks: EditorCallbacks, modifier: Modifier) {
    LaunchedEffect(Unit) { host.feedback(true) }
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(title, stringResource(R.string.contributions_back), onBack = callbacks.onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                CoffeePeekButton(stringResource(R.string.contributions_open_request), { callbacks.onOpenRequest(request.id) })
                OutlinedButton(onClick = callbacks.onBack, modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight)) { Text(stringResource(R.string.contributions_roaster_done)) }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            Image(painterResource(R.drawable.contributions_mascot_happy), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Text(
                stringResource(if (editing) R.string.contributions_editor_saved_title else R.string.contributions_editor_sent_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            Text(
                stringResource(
                    when (request.status) {
                        ModerationStatus.Approved -> R.string.contributions_editor_status_approved
                        ModerationStatus.Rejected -> R.string.contributions_editor_status_rejected
                        ModerationStatus.Pending -> R.string.contributions_editor_status_pending
                    },
                ),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val noCallbacks = EditorCallbacks({}, {}, {})

@PreviewLightDark @Composable private fun EditorDescriptionPreview() = CoffeePeekTheme {
    Surface {
        ChangeEditorContent(
            ChangeEditorState(ChangeSection.Description, signedIn = true, isLoading = false, shopTitle = "Sandbox Cafe", description = "Новое описание кофейни"),
            editing = false, ChangeEditorHost(), noCallbacks, {},
        )
    }
}

@PreviewLightDark @Composable private fun EditorContactsPreview() = CoffeePeekTheme {
    Surface {
        ChangeEditorContent(
            ChangeEditorState(ChangeSection.Contacts, signedIn = true, isLoading = false, shopTitle = "Sandbox Cafe", phone = "abc", website = "sandbox.by"),
            editing = true, ChangeEditorHost(), noCallbacks, {},
        )
    }
}

@PreviewLightDark @Composable private fun EditorFailedPreview() = CoffeePeekTheme {
    Surface {
        ChangeEditorContent(ChangeEditorState(ChangeSection.Menu, signedIn = true, isLoading = false, submit = EditorSubmit.Failed(null)), editing = false, ChangeEditorHost(), noCallbacks, {})
    }
}
