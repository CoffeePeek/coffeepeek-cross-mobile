package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.Image
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_mascot_happy
import com.coffeepeek.feature.contributions.impl.resources.contributions_back
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_failed_default
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_failed_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_not_found
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_save
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_saved_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_saving_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_sending_description
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_sending_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_sent_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_sign_in_prompt
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_status_approved
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_status_pending
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_status_rejected
import com.coffeepeek.feature.contributions.impl.resources.contributions_load_failed
import com.coffeepeek.feature.contributions.impl.resources.contributions_loading
import com.coffeepeek.feature.contributions.impl.resources.contributions_open_request
import com.coffeepeek.feature.contributions.impl.resources.contributions_retry
import com.coffeepeek.feature.contributions.impl.resources.contributions_roaster_done
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_back_to_form
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_failed_kept
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_retry_send
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_submit
import com.coffeepeek.feature.contributions.impl.resources.contributions_sign_in
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import com.coffeepeek.core.designsystem.component.coffeePeekViewportBottomPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        state.signedIn == false -> FormSignInPrompt(title, stringResource(Res.string.contributions_editor_sign_in_prompt), callbacks.onBack, callbacks.onSignIn, modifier)
        submit is EditorSubmit.Done -> EditorSuccess(title, submit.request, editing, host, callbacks, modifier)
        submit == EditorSubmit.Sending -> SubmissionStatusScreen(
            title, SubmissionStatus.Sending, stringResource(if (editing) Res.string.contributions_editor_saving_title else Res.string.contributions_editor_sending_title),
            stringResource(Res.string.contributions_editor_sending_description), callbacks.onBack, host.feedback, modifier,
        )
        submit is EditorSubmit.Failed -> SubmissionStatusScreen(
            title, SubmissionStatus.Error, stringResource(Res.string.contributions_editor_failed_title),
            "${submit.serverMessage ?: stringResource(Res.string.contributions_editor_failed_default)}\n\n${stringResource(Res.string.contributions_shop_failed_kept)}",
            { onAction(ChangeEditorAction.DismissFailure) }, host.feedback, modifier,
        ) {
            SubmissionErrorActions(
                stringResource(Res.string.contributions_shop_retry_send), stringResource(Res.string.contributions_shop_back_to_form),
                { onAction(ChangeEditorAction.Submit) }, { onAction(ChangeEditorAction.DismissFailure) },
            )
        }
        else -> ChangeEditorForm(state, editing, host, callbacks, onAction, modifier)
    }
}

@Composable
private fun ChangeEditorForm(
    state: ChangeEditorState, editing: Boolean, host: ChangeEditorHost, callbacks: EditorCallbacks,
    onAction: (ChangeEditorAction) -> Unit, modifier: Modifier,
) {
    val title = state.section.title()
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(title, stringResource(Res.string.contributions_back), onBack = callbacks.onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.signedIn == null || state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CoffeePeekLoader(stringResource(Res.string.contributions_loading)) }
            state.loadError != null -> LoadFailure(state, onAction, callbacks, Modifier.padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).coffeePeekScreenBottomPadding().imePadding().padding(horizontal = CoffeePeekDimens.spacing4)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (state.shopTitle.isNotBlank()) {
                        Text(state.shopTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
                    }
                    ChangeEditorBody(state, host, onAction)
                    Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                }
                CoffeePeekButton(
                    stringResource(if (editing) Res.string.contributions_editor_save else Res.string.contributions_shop_submit),
                    { onAction(ChangeEditorAction.Submit) }, enabled = state.canSubmit,
                )
                Spacer(Modifier.height(CoffeePeekDimens.spacing3))
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
                EditorLoadError.SignInRequired -> stringResource(Res.string.contributions_editor_sign_in_prompt)
                EditorLoadError.NotFound -> state.loadMessage ?: stringResource(Res.string.contributions_editor_not_found)
                else -> state.loadMessage ?: stringResource(Res.string.contributions_load_failed)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        if (state.loadError != EditorLoadError.NotFound) CoffeePeekButton(
            stringResource(if (signIn) Res.string.contributions_sign_in else Res.string.contributions_retry),
            { if (signIn) callbacks.onSignIn() else onAction(ChangeEditorAction.Reload) },
        )
    }
}

@Composable
private fun EditorSuccess(title: String, request: ChangeRequest, editing: Boolean, host: ChangeEditorHost, callbacks: EditorCallbacks, modifier: Modifier) {
    LaunchedEffect(Unit) { host.feedback(true) }
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(title, stringResource(Res.string.contributions_back), onBack = callbacks.onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(Modifier.fillMaxWidth().coffeePeekViewportBottomPadding().coffeePeekScreenBottomPadding().padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                CoffeePeekButton(stringResource(Res.string.contributions_open_request), { callbacks.onOpenRequest(request.id) })
                OutlinedButton(onClick = callbacks.onBack, modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight)) { Text(stringResource(Res.string.contributions_roaster_done)) }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            Image(painterResource(Res.drawable.contributions_mascot_happy), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Text(
                stringResource(if (editing) Res.string.contributions_editor_saved_title else Res.string.contributions_editor_sent_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            Text(
                stringResource(
                    when (request.status) {
                        ModerationStatus.Approved -> Res.string.contributions_editor_status_approved
                        ModerationStatus.Rejected -> Res.string.contributions_editor_status_rejected
                        ModerationStatus.Pending -> Res.string.contributions_editor_status_pending
                    },
                ),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val noCallbacks = EditorCallbacks({}, {}, {})

@Preview @Composable private fun EditorDescriptionPreviewLight() = EditorDescriptionPreview(false)
@Preview @Composable private fun EditorDescriptionPreviewDark() = EditorDescriptionPreview(true)

@Composable private fun EditorDescriptionPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ChangeEditorContent(
            ChangeEditorState(ChangeSection.Description, signedIn = true, isLoading = false, shopTitle = "Sandbox Cafe", description = "Новое описание кофейни"),
            editing = false, ChangeEditorHost(), noCallbacks, {},
        )
    }
}

@Preview @Composable private fun EditorContactsPreviewLight() = EditorContactsPreview(false)
@Preview @Composable private fun EditorContactsPreviewDark() = EditorContactsPreview(true)

@Composable private fun EditorContactsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ChangeEditorContent(
            ChangeEditorState(ChangeSection.Contacts, signedIn = true, isLoading = false, shopTitle = "Sandbox Cafe", phone = "abc", website = "sandbox.by"),
            editing = true, ChangeEditorHost(), noCallbacks, {},
        )
    }
}

@Preview @Composable private fun EditorFailedPreviewLight() = EditorFailedPreview(false)
@Preview @Composable private fun EditorFailedPreviewDark() = EditorFailedPreview(true)

@Composable private fun EditorFailedPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        ChangeEditorContent(ChangeEditorState(ChangeSection.Menu, signedIn = true, isLoading = false, submit = EditorSubmit.Failed(null)), editing = false, ChangeEditorHost(), noCallbacks, {})
    }
}
