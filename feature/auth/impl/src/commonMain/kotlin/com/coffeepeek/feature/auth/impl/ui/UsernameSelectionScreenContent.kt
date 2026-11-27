package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.api.*
import com.coffeepeek.feature.auth.domain.isValidSelectedUsername
import com.coffeepeek.feature.auth.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun UsernameSelectionScreenContent(state: UsernameSelectionState,
    onAction: (UsernameSelectionAction) -> Unit, modifier: Modifier = Modifier,
) {
    CoffeePeekBackHandler(enabled = state.blocksApp) { }
    CoffeePeekScrollableColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(CoffeePeekDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
            Text(stringResource(Res.string.auth_username_title), Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(Res.string.auth_username_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (state.phase) {
                UsernameSelectionPhase.Required, UsernameSelectionPhase.Submitting -> UsernameSelectionEditor(state, onAction)
                UsernameSelectionPhase.Failed -> {
                    Text(stringResource(Res.string.auth_username_unavailable), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    AuthPrimaryButton(stringResource(Res.string.auth_username_retry), { onAction(UsernameSelectionAction.Retry) })
                }
                else -> CoffeePeekLoader()
            }
            CoffeePeekTextButton(onClick = { onAction(UsernameSelectionAction.SignOut) }) {
                Text(stringResource(Res.string.auth_username_sign_out))
            }
        }
    }
}

@Composable
private fun UsernameSelectionEditor(state: UsernameSelectionState, onAction: (UsernameSelectionAction) -> Unit) {
    val busy = state.phase == UsernameSelectionPhase.Submitting
    val label = stringResource(Res.string.auth_username_label)
    val errorText = state.error?.let { stringResource(usernameErrorText(it)) }
    CoffeePeekCompactOutlinedTextField(value = state.username,
        onValueChange = { onAction(UsernameSelectionAction.Edit(it)) }, enabled = !busy,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label; contentType = ContentType.Username },
        isError = errorText != null, errorDescription = errorText, placeholder = { Text(label) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAction(UsernameSelectionAction.Submit) }))
    Text(stringResource(Res.string.auth_username_rules), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(stringResource(Res.string.auth_username_link_notice), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    errorText?.let {
        Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.error)
    }
    AuthPrimaryButton(stringResource(Res.string.auth_username_continue), { onAction(UsernameSelectionAction.Submit) },
        enabled = isValidSelectedUsername(state.username), loading = busy)
}

private fun usernameErrorText(error: UsernameSelectionError) = when (error) {
    UsernameSelectionError.Invalid -> Res.string.auth_username_invalid
    UsernameSelectionError.Taken -> Res.string.auth_username_taken
    UsernameSelectionError.Conflict -> Res.string.auth_username_conflict
    UsernameSelectionError.Unavailable -> Res.string.auth_username_unavailable
}

@Preview
@Composable
private fun UsernameSelectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    UsernameSelectionScreenContent(UsernameSelectionState(UsernameSelectionPhase.Required, username = "coffee_lover"), {})
}

@Preview
@Composable
private fun UsernameSelectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    UsernameSelectionScreenContent(UsernameSelectionState(UsernameSelectionPhase.Required, username = "coffee_lover",
        error = UsernameSelectionError.Taken), {})
}
